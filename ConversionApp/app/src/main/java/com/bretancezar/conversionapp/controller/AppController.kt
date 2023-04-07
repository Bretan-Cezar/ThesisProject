package com.bretancezar.conversionapp.controller

import android.app.Application
import android.util.Log
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.service.RecorderService
import com.bretancezar.conversionapp.service.RetrofitService
import com.bretancezar.conversionapp.service.StorageService
import com.bretancezar.conversionapp.service.dto.ConversionDTO
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException
import javax.inject.Inject


class AppController @Inject constructor(
    private val retrofit: RetrofitService,
    private val recorder: RecorderService,
    private val storage: StorageService,
    private val context: Application) {

    private var currentRecording: Recording? = null

    var awaitingResponse = MutableStateFlow(false)

    suspend fun startRecording() {

        currentRecording = recorder.startRecording()

        Log.i("RECORDER", "Device started recording to file ${currentRecording!!.filename}.")
    }

    suspend fun stopRecording() {

        if (currentRecording != null) {

            recorder.stopRecording()
            Log.i("RECORDER", "Device stopped recording to file ${currentRecording!!.filename}.")
        }
    }

    fun saveOriginalRecording(): Recording? {

        var ret: Recording? = null

        if (currentRecording != null) {

            ret = storage.addOriginalRecording(currentRecording!!)
            currentRecording = null
        }

        return ret
    }


    fun abortOriginalRecording() {

        if (currentRecording != null) {

            storage.deleteRecordingFile(storage.getRecordingFile(currentRecording!!.speakerClass, currentRecording!!.filename))
            currentRecording = null
        }
    }

    fun renameRecording(id: Long, newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {

        try {

            storage.renameRecording(id, newName)
            onSuccess()
        }
        catch (e: IllegalArgumentException) {

            Log.e("STORAGE", e.stackTraceToString())
            e.message?.let { onFailure(it) }
        }
        catch (e: IllegalStateException) {

            Log.e("STORAGE", e.stackTraceToString())
            e.message?.let { onFailure(it) }
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

    fun conversionAPICall(
        filename: String,
        targetSpeaker: SpeakerClass,
        onSuccess: () -> Unit,
        onNetworkFailure: (String) -> Unit
    ) {

        val bytes = storage.readRecording(SpeakerClass.ORIGINAL, filename)

        Log.i("AppController", "Successfully read recording $filename from local storage.")

        val dto = ConversionDTO(
            targetSpeaker = targetSpeaker.toString(),
            audioFormat = RecorderService.FILE_FORMAT,
            sampleRate = RecorderService.RECORDER_SAMPLE_RATE,
            audioData = bytes
        )

        retrofit.convert(dto).enqueue(object : Callback<ConversionDTO> {

            override fun onResponse(call: Call<ConversionDTO>, response: Response<ConversionDTO>) {

                if (response.isSuccessful){

                    val responseDto = response.body()

                    if (responseDto != null) {

                        try {

                            storage.addReceivedRecording(responseDto.audioData, SpeakerClass.valueOf(responseDto.targetSpeaker))
                            awaitingResponse.value = false
                            Log.i("NETWORK", "Server successfully responded.")
                            onSuccess()
                        }
                        catch (e: IllegalArgumentException) {

                            Log.e("STORAGE", e.stackTraceToString())
                        }
                    }
                    else {

                        awaitingResponse.value = false
                        onNetworkFailure("Network error.")

                        Log.e("NETWORK", "Server error; bad response.")
                    }
                }
                else {

                    awaitingResponse.value = false
                    onNetworkFailure("Network error.")
                    Log.e("NETWORK", "Server error; returned response code ${response.code()}.")
                }
            }
            override fun onFailure(call: Call<ConversionDTO>, t: Throwable) {

                awaitingResponse.value = false
                onNetworkFailure("Network error.")
                Log.e("NETWORK", t.stackTraceToString())
            }
        })

        awaitingResponse.value = true
    }
}