from io import BufferedWriter
from typing import List, Tuple
import numpy as np
import pyflac


class AudioCompressor:

    @staticmethod
    def decompress(file: BufferedWriter) -> Tuple[np.ndarray, int]:
        return pyflac.FileDecoder(input_file=file, output_file=None, compression_level=8).process()

    @staticmethod
    def compress(file: BufferedWriter) -> bytes:
        return pyflac.FileEncoder(input_file=file, output_file=None, compression_level=8).process()
    


class AudioPreprocessor:

    @staticmethod
    def preprocess_waveform(waveform: np.ndarray, sr: int = 16000) -> List[np.ndarray]:

        melspec_list = List[np.ndarray]()

        split_waveform = AudioPreprocessor.__split_waveform(waveform, sr)

        for segment in split_waveform:

            melspec = AudioPreprocessor.__preprocess_segment(segment)
            melspec_list.append(melspec)

        return melspec_list


    @staticmethod
    def __preprocess_segment(waveform: np.ndarray) -> np.ndarray:
        pass


    @staticmethod
    def __split_waveform(waveform: np.ndarray, sr: int, max_length: float = 4.0) -> List[np.ndarray]:
        pass



class VoiceConverter:

    def __init__(self):
        pass


    def convert(self, melspec_list: List[np.ndarray], target) -> tuple[np.ndarray, int]:
        
        segment_list = List[np.ndarray]()

        for melspec in melspec_list:

            converted_waveform = VoiceConverter.__convert_segment(melspec)
            segment_list.append(converted_waveform)

        return VoiceConverter.__join_segments(segment_list)


    def __convert_segment(self, melspec: np.ndarray) -> np.ndarray:
        pass


    def __join_segments(self, segments: List[np.ndarray]) -> np.ndarray:
        pass

