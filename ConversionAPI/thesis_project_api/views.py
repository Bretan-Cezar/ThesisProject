from rest_framework.views import APIView
from django.http import JsonResponse, HttpResponse, HttpResponseBadRequest
import soundfile as sf
from time import time_ns
from service.utils import AudioCompressor, AudioPreprocessor, VoiceConverter
import numpy as np

class ConversionResponseView(APIView):

    def __init__(self):
        self.__compressor = AudioCompressor()
        self.__preprocessor = AudioPreprocessor()
        self.__converter = VoiceConverter()

    # Facade?
    def post(self, request, *args, **kwargs) -> HttpResponse:
        """
        TODO add method description

        @input - request (JSON): "targetSpeaker" - the desired speaker class for conversion
                                 "audioFormat" - "WAV"/"FLAC"
                                 "sampleRate" - sample rate in Hz of the source audio
                                 "audioData" - WAV/FLAC data, file headers included

        @output - JsonResponse: "targetSpeaker" - the speaker class of the converted audio
                                "audioFormat" - "WAV"/"FLAC"
                                "sampleRate" - sample rate in Hz of the source audio
                                "audioData" - WAV/FLAC data, file headers included
        """

        request_data = {
            "targetSpeaker": str(request.data.get('targetSpeaker')),
            "audioFormat": str(request.data.get('audioFormat')),
            "sampleRate": str(request.data.get('sampleRate')),
            "audioData": bytes(request.data.get('audioData')),
        }

        now = str(time_ns())

        waveform: np.ndarray
        input_sample_rate: int

        if request_data["audioFormat"] == 'FLAC':

            # Write source FLAC and decompress to WAV
            with open(f'./files/inputs/input-{now}.flac', 'wb') as f:
                f.write(request_data["audioData"])
                waveform, input_sample_rate = self.__compressor.decompress(f)
        
        elif request_data["audioFormat"] == 'WAV':

            # Write source WAV
            with open(f'./files/inputs/input-{now}.wav', 'wb') as f:
                sf.write(f, request_data["audioData"], input_sample_rate)
                waveform, input_sample_rate = sf.read(f)

        else:
            return HttpResponseBadRequest()
        

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


        return JsonResponse(response_data)
