from typing import List
import numpy as np
import json
import torch
import os
from convs2s import net 

class SpectrogramConverter:

    def __init__(
            self,
            mapper_path: str, 
            mapper_model_file: str, 
            mapper_config_file: str, 
            attention_mode: str,
            device):

        self.__device = device
        self.__loaded: bool = False

        self.__mapper_path = mapper_path
        self.__mapper_model_file = mapper_model_file
        self.__attention_mode = attention_mode

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


    @property
    def loaded(self):
        return self.__loaded

    def load(self):

        if not self.__loaded:

            state_dict = torch.load(os.path.join(self.__mapper_path, self.__mapper_model_file), map_location=self.__device)
            self.__mapper_model.load_state_dict(state_dict['model_state_dict'])
            self.__mapper_model.to(self.__device).eval()

            self.__loaded = True

    def unload(self):

        del self.__mapper_model
        self.__loaded = False

    
        
    def convert(self, melspec_list: List[np.ndarray], target: str) -> List[np.ndarray]:
        
        if self.__loaded:
            converted_melspec_list: List[np.ndarray] = list()
            
            for melspec in melspec_list:

                converted_melspec = self.__convert_melspec(self.__attention_mode, melspec, target)
                converted_melspec_list.append(converted_melspec)

            return converted_melspec_list

        raise RuntimeError("Mapper model currently not loaded.")


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
        
        return conv_melspec

