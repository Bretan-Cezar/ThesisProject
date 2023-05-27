import os, yaml
from nemo.collections.tts.models import HifiGanModel
import torch
import numpy as np
from typing import List, Tuple


class SpectrogramVocoder:

    def __init__(
            self,
            vocoder_path: str, 
            vocoder_model_file: str, 
            vocoder_config_file: str, 
            device):

        self.__device = device
        self.__loaded = False

        self.__vocoder_path = vocoder_path
        self.__vocoder_model_file = vocoder_model_file

        with open(os.path.join(vocoder_path, vocoder_config_file)) as f:
            self.__vocoder_config = yaml.load(f, Loader=yaml.Loader)

        self.__vocoder_model: HifiGanModel = None

    @property
    def loaded(self):
        return self.__loaded

    def load(self):

        if not self.__loaded:

            self.__vocoder_model: HifiGanModel = HifiGanModel.restore_from(os.path.join(self.__vocoder_path, self.__vocoder_model_file)).to(self.__device)
            self.__loaded = True

    def unload(self):

        del self.__vocoder_model
        self.__loaded = False


    def vocode(self, conv_melspec_list: List[np.ndarray]) -> Tuple[np.ndarray, int]:

        if self.__loaded:
            segment_list: List[np.ndarray] = list()

            for melspec in conv_melspec_list:
                
                synthesized_segment = self.__synthesize_segment(melspec)
                segment_list.append(synthesized_segment)


            return SpectrogramVocoder.__join_segments(segment_list), self.__vocoder_config["sample_rate"]

        raise RuntimeError("Mapper model currently not loaded.")


    @torch.no_grad()
    def __synthesize_segment(self, melspec) -> np.ndarray:

        ## HiFiGAN

        melspec = torch.tensor(np.reshape(melspec, (1,) + melspec.shape), dtype=torch.float).to(self.__device)

        #start = time.time()
        x = self.__vocoder_model.convert_spectrogram_to_audio(spec=melspec).cpu().numpy()

        del melspec

        return x[0]

    
    @staticmethod
    def __join_segments(segments: List[np.ndarray]) -> np.ndarray:
        
        return np.concatenate(tuple(segments), dtype=np.float32)