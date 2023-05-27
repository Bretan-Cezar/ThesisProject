import torch
import torch
from nemo.collections.asr.parts.preprocessing.features import FilterbankFeatures
from torch import nn
from sklearn.preprocessing import StandardScaler
import pickle
import os

CONSTANT = 1e-5

class NormalizableFilterbankFeatures(FilterbankFeatures):

    def __init__(self,
        statistics_filepath: str | None = None,
        sample_rate=16000,
        n_window_size=1024,
        n_window_stride=256,
        window="hann",
        normalize="per_spec",
        n_fft=1024,
        preemph=None,
        nfilt=80,
        lowfreq=0,
        highfreq=8000,
        log=True,
        log_zero_guard_type="add",
        log_zero_guard_value=1e-05,
        dither=0.0,
        pad_to=0,
        max_duration=120.0,
        frame_splicing=1,
        exact_pad=True,
        pad_value=-11.52,
        mag_power=1.0,
        use_grads=False,
        rng=None,
        nb_augmentation_prob=0.0,
        nb_max_freq=4000,
        stft_exact_pad=False,  # Deprecated arguments; kept for config compatibility
        stft_conv=False,  # Deprecated arguments; kept for config compatibility
    ):
        super().__init__(
            sample_rate=sample_rate,
            n_window_size=n_window_size,
            n_window_stride=n_window_stride,
            window=window,
            normalize=normalize,
            n_fft=n_fft,
            preemph=preemph,
            nfilt=nfilt,
            lowfreq=lowfreq,
            highfreq=highfreq,
            log=log,
            log_zero_guard_type=log_zero_guard_type,
            log_zero_guard_value=log_zero_guard_value,
            dither=dither,
            pad_to=pad_to,
            max_duration=max_duration,
            frame_splicing=frame_splicing,
            exact_pad=exact_pad,
            pad_value=pad_value,
            mag_power=mag_power,
            use_grads=use_grads,
            rng=rng,
            nb_augmentation_prob=nb_augmentation_prob,
            nb_max_freq=nb_max_freq,
            stft_exact_pad=stft_exact_pad,
            stft_conv=stft_conv,
        )
        
        if (normalize != None and normalize == "by_statistics"):
            self._features_scaler = StandardScaler()

            if os.path.exists(statistics_filepath):
                with open(statistics_filepath, mode='rb') as f:
                    self._features_scaler = pickle.load(f)
            else:
                raise Exception("Statistics file not found.")


    @staticmethod
    def splice_frames(x, frame_splicing):
        """ Stacks frames together across feature dim

        input is batch_size, feature_dim, num_frames
        output is batch_size, feature_dim*frame_splicing, num_frames

        """
        seq = [x]
        for n in range(1, frame_splicing):
            seq.append(torch.cat([x[:, :, :n], x[:, :, n:]], dim=2))
        return torch.cat(seq, dim=1)


    def _normalize_features(self, x: torch.Tensor, seq_len: torch.Tensor):
        
        if self.normalize == "per_melspec":

            x_mean = torch.zeros(seq_len.shape, dtype=x.dtype, device=x.device)
            x_std = torch.zeros(seq_len.shape, dtype=x.dtype, device=x.device) 

            for i in range(x.shape[0]):
                x_mean[i] = x[i, :, : seq_len.int().numpy()[i]].mean()
                x_std[i] = x[i, :, : seq_len.int().numpy()[i]].std()

            # make sure x_std is not zero
            x_std += CONSTANT
            return (x - x_mean.view(-1, 1, 1)) / x_std.view(-1, 1, 1)
        
        elif self.normalize == "by_statistics":

            x = x.numpy()

            for i in range(x.shape[0]):
                x[i, :, :seq_len.int().numpy()[i]] = self._features_scaler.transform(x[i, :, :seq_len.int().numpy()[i]].T).T
            
            return torch.tensor(x)
        
        else:
            raise ValueError("Invalid normalization technique")



    def forward(self, x, seq_len, linear_spec=False):

        seq_len = torch.floor((seq_len - self.n_fft) / self.hop_length) + 1

        if self.dither > 0:
            x += self.dither * torch.randn_like(x)

        # disable autocast to get full range of stft values
        with torch.cuda.amp.autocast(enabled=False):
            x = self.stft(x)

        # torch stft returns complex tensor (of shape [B,N,T]); so convert to magnitude
        # guard is needed for sqrt if grads are passed through
        guard = 0 if not self.use_grads else CONSTANT
        x = torch.view_as_real(x)
        x = torch.sqrt(x.pow(2).sum(-1) + guard)

        if self.training and self.nb_augmentation_prob > 0.0:
            for idx in range(x.shape[0]):
                if self._rng.random() < self.nb_augmentation_prob:
                    x[idx, self._nb_max_fft_bin :, :] = 0.0

        # get power spectrum
        if self.mag_power != 1.0:
            x = x.pow(self.mag_power)

        # return plain spectrogram if required
        if linear_spec:
            return x, seq_len

        # dot with filterbank energies
        x = torch.matmul(self.fb.to(x.dtype), x)
        # log features if required
        if self.log:
            if self.log_zero_guard_type == "add":
                x = torch.log(x + self.log_zero_guard_value_fn(x))
            elif self.log_zero_guard_type == "clamp":
                x = torch.log(torch.clamp(x, min=self.log_zero_guard_value_fn(x)))
            else:
                raise ValueError("log_zero_guard_type was not understood")

        # frame splicing if required
        if self.frame_splicing > 1:
            x = NormalizableFilterbankFeatures.splice_frames(x, self.frame_splicing)

        # normalize if required
        if self.normalize != None:
            x = self._normalize_features(x, seq_len)

        # mask to zero any values beyond seq_len in batch, pad to multiple of `pad_to` (for efficiency)
        max_len = x.size(-1)
        mask = torch.arange(max_len).to(x.device)
        mask = mask.repeat(x.size(0), 1) >= seq_len.unsqueeze(1)

        x = x.masked_fill(mask.unsqueeze(1).type(torch.bool).to(device=x.device), self.pad_value)

        del mask

        pad_to = self.pad_to

        if pad_to == "max":
            x = nn.functional.pad(x, (0, self.max_length - x.size(-1)), value=self.pad_value)
        elif pad_to > 0:
            pad_amt = x.size(-1) % pad_to
            if pad_amt != 0:
                x = nn.functional.pad(x, (0, pad_to - pad_amt), value=self.pad_value)
                
        return x, seq_len