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
    }

    suspend fun stopRecording(): Recording? {

        if (currentRecording != null) {

            recorder.stopRecording()
        }

        return currentRecording
    }

    fun renameRecording(id: Long) {

    }

    fun sendToAPI(
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

                        storage.addReceivedRecording(responseDto.audioData, SpeakerClass.valueOf(responseDto.targetSpeaker))

                        awaitingResponse.value = false
                        onSuccess()

                        Log.i("NETWORK", "Server successfully responded.")
                    }
                    else {

                        awaitingResponse.value = false
                        onNetworkFailure("Network error.")

                        Log.i("NETWORK", "Server failed has sent a bad response.")
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