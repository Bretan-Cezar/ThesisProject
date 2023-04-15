package com.bretancezar.conversionapp.controller

import android.util.Log
import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.service.PlayerService
import com.bretancezar.conversionapp.service.RecorderService
import com.bretancezar.conversionapp.service.RetrofitService
import com.bretancezar.conversionapp.service.StorageService
import com.bretancezar.conversionapp.service.dto.ConversionDTO
import com.bretancezar.conversionapp.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.IllegalStateException


class AppController @Inject constructor (
    private val recorder: RecorderService,
    private val retrofit: RetrofitService,
    private val storage: StorageService,
    private val player: PlayerService
) {

    private var currentRecording: Recording? = null

    var awaitingResponse = MutableStateFlow(false)

    fun initPlayer(recording: Recording) {

        try {

            player.initPlayer(recording.filename, recording.speakerClass)
            Log.i("PLAYER", "Successfully initialized player for file ${recording.filename}")
        }
        catch (e: IOException) {

            Log.e("PLAYER", e.stackTraceToString())
        }
    }

    fun startPlayback() {
        player.startPlayer()
        Log.i("PLAYER", "Currently playing audio...")
    }

    fun pausePlayback() {
        player.pausePlayer()
        Log.i("PLAYER", "Paused playback.")
    }

    fun resetPlayback() {
        player.resetPlayer()
        Log.i("PLAYER", "Reset playback to beginning of track.")
    }

    fun seekPlayback(ms: Int) {
        player.setPlayerPosition(ms)
        Log.i("PLAYER", "Seeked to ${ms/1000} seconds within the track.")
    }

    fun destroyPlayer() {

        try {
            player.destroyPlayer()
            Log.i("PLAYER", "Stopped playback and destroyed player instance.")
        }
        catch (e: IllegalStateException) {
            Log.e("PLAYER", e.stackTraceToString())
        }
    }

    fun getRecordingDurationInMs(): Int {

        return player.getMediaLength()
    }

    fun getPlayerElapsedMs(): Int {

        return player.getPlayerPosition()
    }

    fun seekPlayerToSeconds(ms: Int) {

        player.setPlayerPosition(ms)
    }

    fun startRecording() {

        currentRecording = recorder.startRecording()

        Log.i("RECORDER", "Device started recording to file ${currentRecording!!.filename}.")
    }

    fun stopRecording() {

        if (currentRecording != null) {

            recorder.stopRecording()
            Log.i("RECORDER", "Device stopped recording to file ${currentRecording!!.filename}.")
        }
        else {
            Log.w("AppController", "Currently not recording; no stop operation performed.")
        }
    }

    fun saveOriginalRecording(): Recording {

        if (currentRecording != null) {

            val ret = storage.addOriginalRecording(currentRecording!!)
            currentRecording = null

            return ret
        }

        throw IllegalStateException("This should never be thrown")
    }


    fun abortOriginalRecording() {

        if (currentRecording != null) {

            storage.deleteRecordingFile(storage.getRecordingFile(currentRecording!!.speakerClass, currentRecording!!.filename))
            currentRecording = null
        }

        Log.w("AppController", "Currently not recording; no abort operation performed.")
    }

    fun renameRecording(id: Long, newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit): Recording? {

        try {

            val newRec = storage.renameRecording(id, newName)
            onSuccess()

            return newRec
        }
        catch (e: IllegalArgumentException) {

            Log.e("STORAGE", e.stackTraceToString())
            e.message?.let { onFailure(it) }

            return null
        }
        catch (e: IllegalStateException) {

            Log.e("STORAGE", e.stackTraceToString())
            e.message?.let { onFailure(it) }

            return null
        }
    }

    fun deleteRecording(id: Long) {

        try {

            storage.deleteRecording(id)
        }
        catch (e: IllegalArgumentException) {

            Log.e("STORAGE", e.stackTraceToString())
        }
    }

    fun findRecordingsBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>> {

        return storage.getBySpeakerClass(speakerClass)
    }

    fun conversionAPICall(
        filename: String,
        targetSpeaker: SpeakerClass,
        onSuccessUI: () -> Unit,
        onNetworkFailureUI: (String) -> Unit,
        ioScope: CoroutineScope
    ) {

        val bytes = storage.readRecording(SpeakerClass.ORIGINAL, filename)

        Log.i("AppController", "Successfully read recording $filename from local storage.")

        val dto = ConversionDTO(
            targetSpeaker = targetSpeaker.toString(),
            audioFormat = RecorderService.FILE_FORMAT.toString(),
            sampleRate = RecorderService.RECORDER_SAMPLE_RATE,
            audioData = bytes.toBase64()
        )

        retrofit.convert(dto).enqueue(object : Callback<ConversionDTO> {

            override fun onResponse(call: Call<ConversionDTO>, response: Response<ConversionDTO>) {

                if (response.isSuccessful){

                    val responseDto = response.body()

                    if (responseDto != null) {

                        try {

                            ioScope.launch(Dispatchers.IO) {
                                storage.addReceivedRecording(
                                    bytes = responseDto.audioData.fromBase64(),
                                    speakerClass = SpeakerClass.valueOf(responseDto.targetSpeaker),
                                    originalName = filename,
                                    format = AudioFileFormats.valueOf(responseDto.audioFormat)
                                )
                                awaitingResponse.value = false
                            }

                            Log.i("NETWORK", "Server successfully responded.")
                            onSuccessUI()
                        }
                        catch (e: IllegalArgumentException) {

                            Log.e("STORAGE", e.stackTraceToString())
                        }
                    }
                    else {

                        awaitingResponse.value = false
                        onNetworkFailureUI("Network error.")

                        Log.e("NETWORK", "Server error; bad response.")
                    }
                }
                else {

                    awaitingResponse.value = false
                    onNetworkFailureUI("Network error.")
                    Log.e("NETWORK", "Server error; returned response code ${response.code()}.")
                }
            }
            override fun onFailure(call: Call<ConversionDTO>, t: Throwable) {

                awaitingResponse.value = false
                onNetworkFailureUI("Network error.")
                Log.e("NETWORK", t.stackTraceToString())
            }
        })

        awaitingResponse.value = true
    }
}