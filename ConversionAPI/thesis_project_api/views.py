from rest_framework.views import APIView
from django.http import JsonResponse, HttpResponse, HttpResponseBadRequest
import soundfile as sf
from time import time_ns, sleep
from service.utils import AudioCompressor, AudioPreprocessor, VoiceConverter
import numpy as np
import base64

class ConversionResponseView(APIView):

    def __init__(self):
        self.__compressor = AudioCompressor()
        self.__preprocessor = AudioPreprocessor()
        self.__converter = VoiceConverter()

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

        now = str(time_ns())

        waveform: np.ndarray
        input_sample_rate: int = request_data["sampleRate"]

        if request_data["audioFormat"] == 'FLAC':

            # Write source FLAC and decompress to WAV
            with open(f'./files/inputs/input-{now}.flac', 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = self.__compressor.decompress(f)
        
        elif request_data["audioFormat"] == 'WAV':

            # Write source WAV
            with open(f'./files/inputs/input-{now}.wav', 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = sf.read(f.name)

        else:
            return HttpResponseBadRequest()
        
        '''
        # Preprocess WAV to log-mel-spectrograms list
        melspec_list = self.__preprocessor.preprocess_waveform(waveform, input_sample_rate)

        # Convert source log-mel-spectrograms to target speaker WAV
        output_waveform, conv_sample_rate = self.__converter.convert(melspec_list, request_data["targetSpeaker"])

        output_data: bytes
             
        with open(f'./files/converted/conv-{now}.wav', 'wb') as f:

            if request_data["audioFormat"] == 'FLAC':
                # Write converted WAV and compress to FLAC

                sf.write(f, output_waveform, conv_sample_rate)
                output_data = self.__compressor.compress(f)

            elif request_data["audioFormat"] == 'WAV':
                # Write converted WAV

                sf.write(f, output_waveform, conv_sample_rate)
                output_data = f.read()
                

        response_data = {
            "targetSpeaker": request_data["targetSpeaker"],
            "audioFormat": request_data["audioFormat"],
            "sampleRate": conv_sample_rate,
            "audioData": output_data
        }
'''

        response_data = {
            "targetSpeaker": request_data["targetSpeaker"],
            "audioFormat": request_data["audioFormat"],
            "sampleRate": request_data["sampleRate"],
            "audioData": str(request.data.get('audioData'))
        }

        return JsonResponse(response_data)
