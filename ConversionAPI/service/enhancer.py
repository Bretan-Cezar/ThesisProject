import os
from nemo.collections.tts.models import SpectrogramEnhancerModel
import torch
import numpy as np
from typing import List


class SpectrogramEnhancer:

    def __init__(
            self,
            enhancer_path: str, 
            enhancer_model_file: str,
            device):

        self.__device = device
        self.__loaded = False

        self.__enhancer_path = enhancer_path
        self.__enhancer_model_file = enhancer_model_file

        self.__enhancer_model: SpectrogramEnhancerModel = None

    @property
    def loaded(self):
        return self.__loaded

    def load(self):

        if not self.__loaded:

            self.__enhancer_model: SpectrogramEnhancerModel = SpectrogramEnhancerModel.restore_from(
                os.path.join(self.__enhancer_path, self.__enhancer_model_file)).to(self.__device)
            self.__loaded = True

    def unload(self):

        del self.__enhancer_model
        self.__loaded = False


    def enhance(self, conv_melspec_list: List[np.ndarray]) -> List[np.ndarray]:

        if self.__loaded:
            segment_list: List[np.ndarray] = list()

            for melspec in conv_melspec_list:
                
                enhanced_segment = self.__enhance_segment(melspec)
                segment_list.append(enhanced_segment)

            return segment_list

        raise RuntimeError("Enhancer model currently not loaded.")


    @torch.no_grad()
    def __enhance_segment(self, conv_melspec) -> np.ndarray:

        conv_melspec = torch.tensor(np.reshape(conv_melspec, (1,) + conv_melspec.shape), dtype=torch.float).to(self.__device)

        enhanced_melspec = self.__enhancer_model.forward(
                                input_spectrograms=conv_melspec, 
                                lengths=torch.tensor([conv_melspec.shape[2]]).to(self.__device)
                            ).cpu().numpy()

        return enhanced_melspec[0]