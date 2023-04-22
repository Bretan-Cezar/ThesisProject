package com.bretancezar.conversionapp.utils

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import java.io.IOException

enum class AudioFileFormats {
    WAV, FLAC
}

fun short2byte(sData: ShortArray): ByteArray {

    val arrSize = sData.size
    val bytes = ByteArray(arrSize * 2)

    for (i in 0 until arrSize) {

        bytes[i * 2] = (sData[i].toInt() and 0x00FF).toByte()
        bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
        sData[i] = 0
    }

    return bytes
}

/**
 * Constructs header for wav file format
 */
fun wavFileHeader(channels: Short, sampleRate: Int, bitsPerSample: Short, byteRate: Int): ByteArray {

    val headerSize = 44
    val header = ByteArray(headerSize)

    header[0] = 'R'.code.toByte() // RIFF/WAVE header
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()

    header[4] = (0 and 0xff).toByte() // Size of the overall file, 0 because unknown
    header[5] = (0 shr 8 and 0xff).toByte()
    header[6] = (0 shr 16 and 0xff).toByte()
    header[7] = (0 shr 24 and 0xff).toByte()

    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()

    header[12] = 'f'.code.toByte() // 'fmt ' chunk
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()

    header[16] = 16 // Length of format data
    header[17] = 0
    header[18] = 0
    header[19] = 0

    header[20] = 1 // Type of format (1 is PCM)
    header[21] = 0

    header[22] = channels.toByte()
    header[23] = 0

    header[24] = (sampleRate and 0xff).toByte() // Sampling rate
    header[25] = (sampleRate shr 8 and 0xff).toByte()
    header[26] = (sampleRate shr 16 and 0xff).toByte()
    header[27] = (sampleRate shr 24 and 0xff).toByte()

    header[28] = (byteRate and 0xff).toByte() // Byte rate = (Sample Rate * BitsPerSample * Channels) / 8
    header[29] = (byteRate shr 8 and 0xff).toByte()
    header[30] = (byteRate shr 16 and 0xff).toByte()
    header[31] = (byteRate shr 24 and 0xff).toByte()

    header[32] = (channels * bitsPerSample / 8).toByte() //  16 Bits stereo
    header[33] = 0

    header[34] = bitsPerSample.toByte() // Bits per sample
    header[35] = 0

    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()

    header[40] = (0 and 0xff).toByte() // Size of the data section.
    header[41] = (0 shr 8 and 0xff).toByte()
    header[42] = (0 shr 16 and 0xff).toByte()
    header[43] = (0 shr 24 and 0xff).toByte()

    return header
}

fun updateWAVHeaderInformation(data: ArrayList<Byte>) {
    val fileSize = data.size
    val contentSize = fileSize - 44

    data[4] = (fileSize and 0xff).toByte() // Size of the overall file
    data[5] = (fileSize shr 8 and 0xff).toByte()
    data[6] = (fileSize shr 16 and 0xff).toByte()
    data[7] = (fileSize shr 24 and 0xff).toByte()

    data[40] = (contentSize and 0xff).toByte() // Size of the data section.
    data[41] = (contentSize shr 8 and 0xff).toByte()
    data[42] = (contentSize shr 16 and 0xff).toByte()
    data[43] = (contentSize shr 24 and 0xff).toByte()
}


// TODO: Raw Wave data to FLAC header + encoded data
fun wav2flac(data: ByteArray, channels: Short, sampleRate: Int, bitsPerSample: Short, frameSize: Int): ByteArray {

    val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)

    val outBuf = ByteArray(data.size)

    val format = MediaFormat()
    format.setString(MediaFormat.KEY_MIME, "audio/flac")
    format.setInteger(MediaFormat.KEY_BIT_RATE, sampleRate * channels * bitsPerSample)
    format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
    format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels.toInt())

    val codecName = mcl.findEncoderForFormat(format)

    var codec: MediaCodec? = null

    try {

        codec = MediaCodec.createByCodecName(codecName)
    }
    catch (e: IOException) {
        Log.e("CODEC", e.stackTraceToString())
    }

    codec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

    val usec: Long = 1000000000L * frameSize / sampleRate

    var outputFormat = codec.outputFormat // option B
    val bufInfo = MediaCodec.BufferInfo()
    bufInfo[0, frameSize * channels * 2, usec] = 0

    codec.start()

    var encoded = 0
    val inputBufferId = codec.dequeueInputBuffer(1000)

    if (inputBufferId >= 0) {

        val inputBuffer = codec.getInputBuffer(inputBufferId)

        // fill inputBuffer with valid data
        inputBuffer!!.put(data, 0, data.size)

        codec.queueInputBuffer(inputBufferId, 0, data.size, usec, 0)
    }

    val outputBufferId = codec.dequeueOutputBuffer(bufInfo, 1000)

    if (outputBufferId >= 0) {

        val outputBuffer = codec.getOutputBuffer(outputBufferId)

        val bufferFormat = codec.getOutputFormat(outputBufferId) // option A
        // bufferFormat is identical to outputFormat
        // outputBuffer is ready to be processed or rendered.

        outputBuffer!!.rewind()

        encoded = outputBuffer.remaining()

        outputBuffer[outBuf, 0, encoded]

        codec.releaseOutputBuffer(outputBufferId, false)

    }

    if (encoded > 0) {


    }

    return outBuf
}