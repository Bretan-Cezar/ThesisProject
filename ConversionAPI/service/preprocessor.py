from typing import List
import numpy as np
import json
import librosa
import torch
from .featurizer import NormalizableFilterbankFeatures

class AudioPreprocessor:

    def __init__(self, data_config_path: str, device):

        self.__device = device
        
        with open(data_config_path) as f:
            self.__data_config = json.load(f)

        self.__featurizer = NormalizableFilterbankFeatures(
                normalize='per_melspec',
                statistics_filepath=None,
                n_window_size=self.__data_config['flen'], 
                n_window_stride=self.__data_config['fshift'], 
                lowfreq=self.__data_config['fmin'], 
                highfreq=self.__data_config['fmax'], 
                nfilt=self.__data_config['num_mels'])
            

    def preprocess_waveform(self, waveform: np.ndarray, sr: int = 32000) -> List[np.ndarray]:

        melspec_list: List[np.ndarray] = list()

        split_waveform = AudioPreprocessor.__split_waveform(waveform, sr)

        for segment in split_waveform:

            melspec = self.__preprocess_segment(segment, sr)
            melspec_list.append(melspec)

        return melspec_list


    def __preprocess_segment(self, waveform: np.ndarray, sr: int) -> np.ndarray:
        
        return self.__extract_melspec(waveform, sr)
        
    
    def __extract_melspec(self, waveform: np.ndarray, sr_: int):

        trim_silence = self.__data_config['trim_silence']
        top_db = self.__data_config['top_db']
        sr = self.__data_config['sr']

        if trim_silence:
            waveform, _ = librosa.effects.trim(waveform, top_db=top_db, frame_length=2048, hop_length=512)
        if sr != sr_:
            waveform = librosa.resample(y=waveform, orig_sr=sr_, target_sr=sr)
        
        melspec, _ = self.__featurizer.forward(torch.tensor(waveform), torch.tensor([waveform.shape[0]]))

        melspec = melspec.numpy()[0].astype(np.float32) # n_mels x n_frame

        # plt.matshow(melspec)
        # plt.show()

        b_melspec = np.reshape(melspec, (1,) + melspec.shape)

        return torch.tensor(b_melspec).to(self.__device, dtype=torch.float)


    @staticmethod
    def __split_waveform(waveform: np.ndarray, sr: int, max_length: float = 4.0) -> List[np.ndarray]:
        
        segments: List[np.ndarray] = list() 
        samples = waveform.shape[0]

        index = 0
        while index < samples:

            limit = int(index + sr*max_length)

            if limit > samples:

                segments.append(waveform[index:])
                index = samples

            else:

                min_vol = 100000.0
                min_vol_index = limit

                while limit > index + 2.0*sr:

                    if abs(waveform[limit]) < min_vol:
                        min_vol = abs(waveform[limit])
                        min_vol_index = limit

                    limit -= 1

                segments.append(waveform[index:min_vol_index])

                index = min_vol_index

        return segments
            
            
