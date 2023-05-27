from service.compressor import AudioCompressor
from service.preprocessor import AudioPreprocessor
from service.mapper import SpectrogramConverter
from service.vocoder import SpectrogramVocoder
import torch
import json
import os
from typing import Dict

compressor = AudioCompressor()
preprocessor: AudioPreprocessor = None
converters: Dict[str, SpectrogramConverter] = dict()
vocoders: Dict[str, SpectrogramVocoder] = dict()
api_config: Dict = dict()

with open('./thesis_project_api/api_config.json') as conf:
    api_config = json.load(conf)

    gpu = api_config["conversion"]["gpu"]

    files_root = api_config["files"]["path"]

    if torch.cuda.is_available() and gpu >= 0:

        __device = torch.device(f'cuda:{gpu}')
    else:

        __device = torch.device('cpu')

    if __device.type == 'cuda':
        torch.cuda.device(__device)

    preprocessor = AudioPreprocessor(
        os.path.join(files_root, api_config["files"]["data_config"]),
        __device)
    
    mapper_files = api_config["conversion"]["mappers"]["files"]
    vocoder_files = api_config["conversion"]["vocoders"]["files"]

    for mapper_json in mapper_files:
        
        converter = SpectrogramConverter(
            os.path.join(api_config["conversion"]["mappers"]["path"], mapper_json["name"]),
            mapper_json["model"],
            mapper_json["config"],
            mapper_json["attention_mode"],
            __device
        )

        converter.load()
        converters[mapper_json["trg_spk"]] = converter
    
    for vocoder_json in vocoder_files:

        vocoder = SpectrogramVocoder(
            os.path.join(api_config["conversion"]["vocoders"]["path"], vocoder_json["name"]),
            vocoder_json["model"],
            vocoder_json["config"],
            __device
        )  

        vocoder.load()
        vocoders[vocoder_json["trg_spk"]] = vocoder



def unload_converter_models():
    for _, map in converters.items():
        map.unload()


def unload_vocoder_models():
    for _, voc in vocoders.items():
        voc.unload()
