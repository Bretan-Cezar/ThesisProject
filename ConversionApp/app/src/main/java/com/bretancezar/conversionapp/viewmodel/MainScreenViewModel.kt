package com.bretancezar.conversionapp.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bretancezar.conversionapp.R
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
class MainScreenViewModel @Inject constructor (
    private val controller: AppController
): ViewModel() {

    val speakersList: List<String> =
        SpeakerClass
            .values()
            .toList()
            .filter { return@filter it != SpeakerClass.ORIGINAL }.map { it.toString() }


    private var _selectedSpeaker: MutableStateFlow<String?> = MutableStateFlow(null)

    private var _currentRecording: MutableStateFlow<Recording?> = MutableStateFlow(null)

    private var _secondsRecorded: MutableStateFlow<Int> = MutableStateFlow(0)

    private var _recordingButtonState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var _buttonColor: MutableStateFlow<Color> = MutableStateFlow(Color.Green)

    private var _buttonIcon: MutableStateFlow<Int> = MutableStateFlow(R.drawable.baseline_fiber_manual_record_128)

    var selectedSpeaker: StateFlow<String?> = _selectedSpeaker

    var currentRecording: StateFlow<Recording?> = _currentRecording

    var awaitingResponse: StateFlow<Boolean> = controller.awaitingResponse

    var secondsRecorded: StateFlow<Int> = _secondsRecorded
    var recordingButtonState: StateFlow<Boolean> = _recordingButtonState
    var buttonColor: StateFlow<Color> = _buttonColor
    var buttonIcon: StateFlow<Int> = _buttonIcon

    fun setSelectedSpeaker(speaker: String?) {

        _selectedSpeaker.value = speaker
    }

    fun increaseTimer() {

        _secondsRecorded.value++
    }

    fun setCurrentRecording(recording: Recording) {

        _currentRecording.value = recording
    }

    fun startRecording() {

        viewModelScope.launch(Dispatchers.IO) {

            _currentRecording.value = null

            _recordingButtonState.value = true
            _buttonColor.value = Color.Red
            _buttonIcon.value = R.drawable.baseline_stop_128

            controller.startRecording()
        }
    }

    fun stopAndSaveRecording() {

        viewModelScope.launch(Dispatchers.IO) {

            controller.stopRecording()

            val newRecording = controller.saveOriginalRecording()

            _currentRecording.value = newRecording

            _recordingButtonState.value = false
            _buttonColor.value = Color.Green
            _buttonIcon.value = R.drawable.baseline_fiber_manual_record_128
            _secondsRecorded.value = 0
        }
    }

    fun stopAndAbortRecording() {

        viewModelScope.launch(Dispatchers.Main) {

            controller.stopRecording()
            controller.abortOriginalRecording()

            _currentRecording.value = null

            _recordingButtonState.value = false
            _buttonColor.value = Color.Green
            _buttonIcon.value = R.drawable.baseline_fiber_manual_record_128
            _secondsRecorded.value = 0
        }
    }

    fun renameCurrentRecording(newName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            val newRecording = controller.renameRecording(_currentRecording.value!!.id!!, newName, onSuccess, onFailure)

            if (newRecording != null) {

                _currentRecording.value = newRecording
            }
        }
    }

    fun deleteCurrentRecording() {

        viewModelScope.launch(Dispatchers.IO) {

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