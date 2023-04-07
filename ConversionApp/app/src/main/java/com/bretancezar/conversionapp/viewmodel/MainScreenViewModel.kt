package com.bretancezar.conversionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bretancezar.conversionapp.controller.AppController
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val controller: AppController
): ViewModel() {

    val speakersList: List<String> = SpeakerClass.values().toList().filter { return@filter it != SpeakerClass.ORIGINAL }.map { it.toString() }

    private var _selectedSpeaker: MutableStateFlow<String?> = MutableStateFlow(null)

    var selectedSpeaker: StateFlow<String?> = _selectedSpeaker

    private var _currentRecording: MutableStateFlow<Recording?> = MutableStateFlow(null)

    var currentRecording: StateFlow<Recording?> = _currentRecording

    var awaitingResponse: StateFlow<Boolean> = controller.awaitingResponse

    fun setSelectedSpeaker(speaker: String?) {

        _selectedSpeaker.value = speaker
    }

    fun startRecording() {

        _currentRecording.value = null

        viewModelScope.launch(Dispatchers.IO) {

            controller.startRecording()
        }
    }

    fun stopAndSaveRecording() {

        viewModelScope.launch(Dispatchers.IO) {

            controller.stopRecording()
            _currentRecording.value = controller.saveOriginalRecording()
        }
    }

    fun stopAndAbortRecording() {

        viewModelScope.launch(Dispatchers.Main) {

            controller.stopRecording()
            controller.abortOriginalRecording()
        }
    }

    fun renameCurrentRecording(newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            controller.renameRecording(_currentRecording.value!!.id!!, newName, onSuccess, onFailure)
        }
    }

    fun deleteCurrentRecording() {

        viewModelScope.launch {

            controller.deleteRecording(_currentRecording.value!!.id!!)

            _currentRecording.value = null
        }
    }


    fun sendForConversion(recording: Recording, targetSpeakerClass: SpeakerClass, onSuccess: () -> Unit, onNetworkFailure: (String) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            controller.conversionAPICall(recording.filename, targetSpeakerClass, onSuccess, onNetworkFailure)
        }
    }

}