from rest_framework.views import APIView
from django.http import JsonResponse
import soundfile as sf
from time import time_ns
from service.utils import AudioCompressor, AudioPreprocessor, VoiceConverter


class ConversionResponseView(APIView):

    def __init__(self):
        self.__compressor = AudioCompressor()
        self.__preprocessor = AudioPreprocessor()
        self.__converter = VoiceConverter()

    # Facade?
    def post(self, request, *args, **kwargs) -> JsonResponse:
        
        request_data = {
            "target_speaker": str(request.data.get('target_speaker')),
            "audio": bytes(request.data.get('audio')),
        }

        now = str(time_ns())

        # Write source FLAC and decompress to WAV
        with open(f'./files/inputs/input-{now}.flac', 'wb') as f:
            f.write(request_data["audio"])
            waveform, sr = self.__compressor.decompress(f)

        # Preprocess WAV to log-mel-spectrograms list
        melspec_list = self.__preprocessor.preprocess_waveform(waveform, sr)

        # Convert source log-mel-spectrograms to target speaker WAV
        output_waveform = self.__converter.convert(melspec_list, request_data["target_speaker"])

        # Write converted WAV and compress to FLAC 
        with open(f'./files/converted/conv-{now}.wav', 'wb') as f:
            sf.write(f, output_waveform, sr)
            compressed_output = self.__compressor.compress(f)

        response_data = {
            "target_speaker": request_data["target_speaker"],
            "audio": compressed_output
        }

        return JsonResponse(response_data)
