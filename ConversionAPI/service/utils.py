from io import BufferedWriter
from typing import List, Tuple
import numpy as np
import pyflac
import matplotlib.pyplot as pyplot
from sklearn.preprocessing import StandardScaler
import json
import librosa
import torch
import pickle
import os
from convs2s import net
import yaml
from parallel_wavegan.utils import load_model

class AudioCompressor:

    @staticmethod
    def decompress(file: BufferedWriter) -> Tuple[np.ndarray, int]:
        return pyflac.FileDecoder(input_file=file, output_file=None, compression_level=8).process()

    @staticmethod
    def compress(file: BufferedWriter) -> bytes:
        return pyflac.FileEncoder(input_file=file, output_file=None, compression_level=8).process()
    


class AudioPreprocessor:

    def __init__(self, data_config_path: str, melspec_stat_path: str, device):

        with open(melspec_stat_path, mode='rb') as f:
            self.__melspec_scaler: StandardScaler = pickle.load(f)

        self.__device = device

        with open(data_config_path) as f:
            self.__data_config = json.load(f)


    def preprocess_waveform(self, waveform: np.ndarray, sr: int = 32000) -> List[np.ndarray]:

        melspec_list: List[np.ndarray] = list()

        split_waveform = AudioPreprocessor.__split_waveform(waveform, sr)

        for segment in split_waveform:

            melspec = self.__preprocess_segment(segment, sr)
            melspec_list.append(melspec)

        return melspec_list


    def __preprocess_segment(self, waveform: np.ndarray, sr: int) -> np.ndarray:
        
        return self.__audio_transform(waveform, sr)
        

    # Source: https://github.com/kamepong/ConvS2S-VC
    def __audio_transform(self, waveform: np.ndarray, sr: int):

        trim_silence = self.__data_config['trim_silence']
        top_db = self.__data_config['top_db']
        flen = self.__data_config['flen']
        fshift = self.__data_config['fshift']
        fmin = self.__data_config['fmin']
        fmax = self.__data_config['fmax']
        num_mels = self.__data_config['num_mels']
        fs = self.__data_config['fs']

        if trim_silence:

            waveform, _ = librosa.effects.trim(waveform, top_db=top_db, frame_length=2048, hop_length=512)

        if fs != sr:

            waveform = librosa.resample(waveform, orig_sr=sr, target_sr=fs)

        melspec_raw = AudioPreprocessor.__logmelfilterbank(waveform, fs, fft_size=flen,hop_size=fshift,
                                        fmin=fmin, fmax=fmax, num_mels=num_mels)
        
        melspec_raw = melspec_raw.astype(np.float32)

        melspec_norm = self.__melspec_scaler.transform(melspec_raw)
        melspec_norm = melspec_norm.T

        return torch.tensor(melspec_norm[None]).to(self.__device, dtype=torch.float)   



    # Source: https://github.com/kamepong/ConvS2S-VC
    @staticmethod
    def __logmelfilterbank(audio, sr, fft_size=1024, hop_size=256, win_length=None, window="hann", num_mels=80, fmin=None, fmax=None, eps=1e-10):
        """
        Compute log-Mel filterbank feature.
        Args:
            audio (ndarray): Audio signal (T,).
            sampling_rate (int): Sampling rate.
            fft_size (int): FFT size.
            hop_size (int): Hop size.
            win_length (int): Window length. If set to None, it will be the same as fft_size.
            window (str): Window function type.
            num_mels (int): Number of mel basis.
            fmin (int): Minimum frequency in mel basis calculation.
            fmax (int): Maximum frequency in mel basis calculation.
            eps (float): Epsilon value to avoid inf in log calculation.
        Returns:
            ndarray: Log Mel filterbank feature (#frames, num_mels).
        """
        # get amplitude spectrogram
        x_stft = librosa.stft(audio, n_fft=fft_size, hop_length=hop_size,
                            win_length=win_length, window=window, pad_mode="reflect")
        
        spc = np.abs(x_stft).T  # (#frames, #bins)

        # get mel basis
        fmin = 0 if fmin is None else fmin
        fmax = sr / 2 if fmax is None else fmax

        mel_basis = librosa.filters.mel(sr=sr, n_fft=fft_size, n_mels=num_mels, fmin=fmin, fmax=fmax)

        return np.log10(np.maximum(eps, np.dot(spc, mel_basis.T)))


    @staticmethod
    def __split_waveform(waveform: np.ndarray, sr: int, max_length: float = 4.0) -> List[np.ndarray]:
        
        segments: List[np.ndarray] = list() 
        samples = waveform.shape[0]

        index = 0
        while index < samples:

            new_index = index + sr*max_length

            if new_index > samples:

                segments.append(waveform[index:])
                index = samples

            else:

                min_vol = 100000.0
                min_vol_index = new_index

                while waveform[new_index] > 0.01 and new_index > index + 1.5*sr:

                    if waveform[new_index] < min_vol:
                        min_vol = waveform[new_index]
                        min_vol_index = new_index

                    new_index -= 1

                if waveform[new_index] <= 0.01:
                    segments.append(waveform[index:new_index])
                    index = new_index
                else:
                    segments.append(waveform[index:min_vol_index])
                    index = min_vol_index

        return segments
            
            

class VoiceConverter:

    def __init__(
            self, 
            mapper_path: str, 
            mapper_model_file: str, 
            mapper_config_file: str, 
            vocoder_path: str, 
            vocoder_model_file: str,
            vocoder_config_file: str,
            device):

        self.__device = device

        with open(os.path.join(mapper_path, mapper_config_file)) as f:
            self.__model_config = json.load(f)

        num_mels = self.__model_config['num_mels']
        n_spk = self.__model_config['n_spk']
        zdim = self.__model_config['zdim']
        mdim = self.__model_config['mdim']
        kdim = self.__model_config['kdim']
        hdim = self.__model_config['hdim']
        num_layers = self.__model_config['num_layers']
        reduction_factor = self.__model_config['reduction_factor']

        enc = net.Encoder1(num_mels*reduction_factor,n_spk,hdim,zdim,kdim,num_layers)
        predec = net.PreDecoder1(num_mels*reduction_factor,n_spk,hdim,zdim,kdim,num_layers)
        postdec = net.PostDecoder1(zdim*2,n_spk,hdim,num_mels*reduction_factor,mdim,num_layers)
        self.__mapper_model = net.ConvS2S(enc, predec, postdec)

        state_dict = torch.load(os.path.join(mapper_path, mapper_model_file), map_location=device)
        self.__mapper_model.load_state_dict(state_dict['model_state_dict'])
        self.__mapper_model.to(self.__device).eval()

        with open(os.path.join(vocoder_path, vocoder_config_file)) as f:
            self.__vocoder_config = yaml.load(f, Loader=yaml.Loader)

        # self.vocoder_config.update(vars(args))

        self.__vocoder_model = load_model(os.path.join(vocoder_path, vocoder_model_file), self.__vocoder_config)
        self.__vocoder_model.remove_weight_norm()
        self.__vocoder_model.to(self.__device).eval()

        
    def convert(self, attention_mode: str, melspec_list: List[np.ndarray], target: str) -> Tuple[np.ndarray, int]:
        
        converted_melspec_list: List[np.ndarray] = list()
        
        for melspec in melspec_list:

            converted_melspec = self.__convert_melspec(attention_mode, melspec, target)
            converted_melspec_list.append(converted_melspec)

        segment_list: List[np.ndarray] = list()

        for melspec in melspec_list:
            
            synthesized_segment = self.__synthesize_segment(melspec).cpu().numpy()
            segment_list.append(synthesized_segment)

        return VoiceConverter.__join_segments(segment_list), self.__vocoder_config["sampling_rate"]


    def __convert_melspec(self, attention_mode, melspec: np.ndarray, target) -> np.ndarray:
        
        source_index = list(self.__model_config['spk_list']).index("czr")
        target_index = list(self.__model_config['spk_list']).index(target)

        with torch.no_grad():
            conv_melspec, A, elapsed_time = self.__mapper_model.inference(
                melspec,
                source_index, 
                target_index, 
                self.__model_config['reduction_factor'], 
                self.__model_config['pos_weight'], 
                attention_mode
            )
        
        return conv_melspec.T # n_frames x n_mels

    @torch.no_grad()
    def __synthesize_segment(self, melspec) -> np.ndarray:

        ## Parallel WaveGAN / MelGAN

        melspec = torch.tensor(melspec[0].T, dtype=torch.float).to(self.__device)

        return self.__vocoder_model.inference(melspec).view(-1)


    @staticmethod
    def __join_segments(segments: List[np.ndarray]) -> np.ndarray:
        
        return np.concatenate(tuple(segments), dtype=np.float32)



    
    

