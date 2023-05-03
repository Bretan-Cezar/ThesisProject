from rest_framework.views import APIView
from django.http import JsonResponse, HttpResponse, HttpResponseBadRequest
import soundfile as sf
from time import time_ns, sleep
from service.utils import AudioCompressor, AudioPreprocessor, VoiceConverter
import numpy as np
import base64
import matplotlib.pyplot as pyplot
import torch
import json
import os

class ConversionResponseView(APIView):

    def __init__(self):

        with open('./thesis_project_api/api_config.json') as conf:
            self.__api_config = json.load(conf)

            gpu = self.__api_config["conversion"]["gpu"]

            files_root = self.__api_config["files"]["path"]
            mapper_root = self.__api_config["conversion"]["mapper"]["path"]
            vocoder_root = self.__api_config["conversion"]["vocoder"]["path"]

            if torch.cuda.is_available() and gpu >= 0:

                self.__device = torch.device(f'cuda:{gpu}')
            else:

                self.__device = torch.device('cpu')

            if self.__device.type == 'cuda':
                torch.cuda.device(self.__device)

            self.__compressor = AudioCompressor()

            self.__preprocessor = AudioPreprocessor(
                os.path.join(files_root, self.__api_config["files"]["data_config"]), 
                os.path.join(files_root, self.__api_config["files"]["scaler_statistics"]), 
                self.__device)
            
            self.__converter = VoiceConverter(
                os.path.join(mapper_root, self.__api_config["conversion"]["mapper"]["ds"], self.__api_config["conversion"]["mapper"]["exp"]),
                self.__api_config["conversion"]["mapper"]["model"],
                self.__api_config["conversion"]["mapper"]["config"],
                os.path.join(vocoder_root, self.__api_config["conversion"]["vocoder"]["name"]),
                self.__api_config["conversion"]["vocoder"]["model"],
                self.__api_config["conversion"]["vocoder"]["config"],
                self.__device
            )



    def post(self, request, *args, **kwargs) -> HttpResponse:
        """
        TODO add method description

        @input - request (JSON): "targetSpeaker" - the desired speaker class for conversion
                                 "audioFormat" - "WAV"/"FLAC"
                                 "sampleRate" - sample rate in Hz of the source audio
                                 "audioData" - base64-encoded WAV/FLAC data, file headers included

        @output - JsonResponse: "targetSpeaker" - the speaker class of the converted audio
                                "audioFormat" - "WAV"/"FLAC"
                                "sampleRate" - sample rate in Hz of the source audio
                                "audioData" - base64-encoded WAV/FLAC data, file headers included
        """

        request_data: dict()

        try:
            request_data = {
                "targetSpeaker": str(request.data.get('targetSpeaker')),
                "audioFormat": str(request.data.get('audioFormat')),
                "sampleRate": str(request.data.get('sampleRate')),
                "audioData": base64.b64decode(str(request.data.get('audioData'))),
            }
        except Exception as te:
            return HttpResponseBadRequest(str(te))

        waveform: np.ndarray
        input_sample_rate: int = request_data["sampleRate"]

        input_files_path = os.path.join(self.__api_config["files"]["path"], self.__api_config["files"]["inputs"])

        now = str(time_ns())
        if request_data["audioFormat"] == 'FLAC':

            # Write source FLAC and decompress to WAV
            with open(os.path.join(input_files_path, f'input-{now}.flac'), 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = self.__compressor.decompress(f)
        
        elif request_data["audioFormat"] == 'WAV':

            # Write source WAV
            with open(os.path.join(input_files_path, f'input-{now}.wav'), 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = sf.read(f.name)

        else:
            return HttpResponseBadRequest()
        
        
        # Preprocess WAV to log-mel-spectrograms list
        melspec_list = self.__preprocessor.preprocess_waveform(waveform, input_sample_rate)

        # Convert source log-mel-spectrograms to target speaker WAV
        output_waveform, conv_sample_rate = self.__converter.convert(
            self.__api_config["conversion"]["mapper"]["attention_mode"],
            melspec_list, 
            request_data["targetSpeaker"]
        )

        output_files_path = os.path.join(self.__api_config["files"]["path"], self.__api_config["files"]["outputs"])
        output_data: bytes
 
        now = str(time_ns())
        with open(os.path.join(output_files_path, f'conv-{now}.wav'), 'wb') as f:
            # Write waveform to WAV file on disk 
            sf.write(f, output_waveform, conv_sample_rate)

        
        with open(os.path.join(output_files_path, f'conv-{now}.wav'), 'rb') as f:
            if request_data["audioFormat"] == 'FLAC':
                # Reload in memory as FLAC compressed file

                output_data = self.__compressor.compress(f)

            elif request_data["audioFormat"] == 'WAV':
                # Reload WAV in memory

                output_data = f.read()


        response_data = {
            "targetSpeaker": request_data["targetSpeaker"],
            "audioFormat": request_data["audioFormat"],
            "sampleRate": conv_sample_rate,
            "audioData": base64.b64encode(output_data).decode('utf-8')
        }
        

        return JsonResponse(response_data)
