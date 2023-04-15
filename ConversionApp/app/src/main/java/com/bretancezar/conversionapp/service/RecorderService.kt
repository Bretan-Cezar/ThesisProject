package com.bretancezar.conversionapp.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.concurrent.thread


class RecorderService @Inject constructor (
    private var _applicationContext: Context
) {

    private var _mediaRecorder: AudioRecord? = null
    private var _active: Boolean = false
    private var _recordingThread: Thread? = null

    companion object {

        const val RECORDER_SAMPLE_RATE = 32000

        // TODO: Change if FLAC conversion works
        val FILE_FORMAT = AudioFileFormats.WAV

        private const val RECORDER_SOURCE = MediaRecorder.AudioSource.MIC
        private const val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        private const val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
        private const val RECORDER_BUFFER_SIZE = 512
        private const val BITS_PER_SAMPLE: Short = 16
        private const val NO_CHANNELS: Short = 1
        private const val BYTE_RATE = RECORDER_SAMPLE_RATE * NO_CHANNELS * BITS_PER_SAMPLE / 8
        private const val ENCODER_FRAME_SIZE = 240
        private const val bufferElements2Rec = 1024
    }

    fun startRecording(): Recording {

        if (!_active) {

            if (ActivityCompat.checkSelfPermission(
                    _applicationContext,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                _mediaRecorder = AudioRecord(
                    RECORDER_SOURCE,
                    RECORDER_SAMPLE_RATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    RECORDER_BUFFER_SIZE)

                val currentDateTime = LocalDateTime.now()

                val filename = "rec-${currentDateTime.formatToFileDateTime()}.wav"

                val output: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                    (_applicationContext.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)!!.absolutePath) + "/${SpeakerClass.ORIGINAL}/$filename"
                }
                else {

                    (_applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath) + "/${SpeakerClass.ORIGINAL}/$filename"
                }

                _mediaRecorder!!.startRecording()
                _active = true

                _recordingThread = thread(true) {

                    writeAudioDataToFile(output, FILE_FORMAT)
                }

                return Recording(
                    id = null,
                    datetime = currentDateTime,
                    speakerClass = SpeakerClass.ORIGINAL,
                    filename = filename)
            }

            throw IllegalStateException("This should not be thrown; permissions for recording audio not granted")
        }

        throw IllegalStateException("This should never be thrown")
    }

    fun stopRecording() {

        try {
            if (_active) {

                _mediaRecorder?.run {
                    _active = false
                    stop()
                    release()
                    _mediaRecorder = null
                    _recordingThread = null
                }
            }
        }
        catch (e: IllegalStateException) {
            Log.e("RecorderService", e.stackTraceToString())
        }
        catch (e: IOException) {
            Log.e("RecorderService", e.stackTraceToString())
        }
    }

    private fun writeAudioDataToFile(path: String, format: AudioFileFormats) {

        val sData = ShortArray(bufferElements2Rec)

        var os: FileOutputStream? = null

        try {

            File(path).createNewFile()
            os = FileOutputStream(path)
        }
        catch (e: FileNotFoundException) {
            Log.e("RecorderService", e.stackTraceToString())
        }

        val data = arrayListOf<Byte>()

        if (format == AudioFileFormats.WAV) {

            for (byte in wavFileHeader(NO_CHANNELS, RECORDER_SAMPLE_RATE, BITS_PER_SAMPLE, BYTE_RATE)) {
                data.add(byte)
            }
        }

        while (_active) {

            // gets the voice output from microphone to byte format
            _mediaRecorder!!.read(sData, 0, bufferElements2Rec)

            try {

                val bData = short2byte(sData)
                for (byte in bData)
                    data.add(byte)
            }
            catch (e: IOException) {
                Log.e("RecorderService", e.stackTraceToString())
            }
        }

        if (format == AudioFileFormats.WAV) {

            updateWAVHeaderInformation(data)
        }

        var dataByteArray = data.toByteArray()

        if (format == AudioFileFormats.FLAC) {

            dataByteArray = wav2flac(dataByteArray, NO_CHANNELS, RECORDER_SAMPLE_RATE, BITS_PER_SAMPLE, ENCODER_FRAME_SIZE)
        }

        os?.write(dataByteArray)

        try {

            os?.close()
        }
        catch (e: IOException) {
            Log.e("RecorderService", e.stackTraceToString())
        }
    }
}