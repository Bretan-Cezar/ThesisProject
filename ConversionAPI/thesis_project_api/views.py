from rest_framework.views import APIView
from django.http import JsonResponse, HttpResponse, HttpResponseBadRequest
import soundfile as sf
from time import time_ns, sleep
import numpy as np
import base64
import service
import os
from typing import Dict

class ConversionResponseView(APIView):

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

        input_files_path = os.path.join(service.api_config["files"]["path"], service.api_config["files"]["inputs"])

        now = str(time_ns())
        if request_data["audioFormat"] == 'FLAC':
            
            # Write source FLAC and decompress to WAV

            return HttpResponse(status=501)
        
            with open(os.path.join(input_files_path, f'input-{now}.flac'), 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = service.compressor.decompress(f)
        
        elif request_data["audioFormat"] == 'WAV':

            # Write source WAV
            with open(os.path.join(input_files_path, f'input-{now}.wav'), 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = sf.read(f.name)

        else:
            return HttpResponseBadRequest()
        
        
        # Preprocess WAV to log-mel-spectrograms list
        melspec_list = service.preprocessor.preprocess_waveform(waveform, input_sample_rate)
        
        # Convert source log-mel-spectrograms to target speaker WAV
        trg_spk = request_data["targetSpeaker"]

        converter = service.converters[trg_spk]
        vocoder = service.vocoders[trg_spk]

        conv_melspec_list = converter.convert(melspec_list, trg_spk)

        del melspec_list

        output_waveform, conv_sample_rate = vocoder.vocode(conv_melspec_list)

        del conv_melspec_list
        
        output_files_path = os.path.join(service.api_config["files"]["path"], service.api_config["files"]["outputs"])
        output_data: bytes
 
        now = str(time_ns())
        with open(os.path.join(output_files_path, f'conv-{now}.wav'), 'wb') as f:
            # Write waveform to WAV file on disk 
            sf.write(f, output_waveform, conv_sample_rate)

        del output_waveform

        with open(os.path.join(output_files_path, f'conv-{now}.wav'), 'rb') as f:
            if request_data["audioFormat"] == 'FLAC':
                # Reload in memory as FLAC compressed file

                output_data = service.compressor.compress(f)

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

