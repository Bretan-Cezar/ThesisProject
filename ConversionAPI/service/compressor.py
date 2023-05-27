from io import BufferedWriter
import pyflac
from typing import Tuple
import numpy as np

class AudioCompressor:

    @staticmethod
    def decompress(file: BufferedWriter) -> Tuple[np.ndarray, int]:
        return pyflac.FileDecoder(input_file=file, output_file=None, compression_level=8).process()

    @staticmethod
    def compress(file: BufferedWriter) -> bytes:
        return pyflac.FileEncoder(input_file=file, output_file=None, compression_level=8).process()