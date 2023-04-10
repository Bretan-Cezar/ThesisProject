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
    var selectedSpeaker: StateFlow<String?> = _selectedSpeaker

    private var _currentRecording: MutableStateFlow<Recording?> = MutableStateFlow(null)
    var currentRecording: StateFlow<Recording?> = _currentRecording

    private var _secondsRecorded: MutableStateFlow<Int> = MutableStateFlow(0)
    var secondsRecorded: StateFlow<Int> = _secondsRecorded

    private var _recordingButtonState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var recordingButtonState: StateFlow<Boolean> = _recordingButtonState

    private var _buttonColor: MutableStateFlow<Color> = MutableStateFlow(Color.Green)
    var buttonColor: StateFlow<Color> = _buttonColor

    private var _buttonIcon: MutableStateFlow<Int> = MutableStateFlow(R.drawable.baseline_fiber_manual_record_128)
    var buttonIcon: StateFlow<Int> = _buttonIcon

    private var _currentRecordingDuration: MutableStateFlow<Int> = MutableStateFlow(0)
    var currentRecordingDuration: StateFlow<Int> = _currentRecordingDuration

    private var _timeElapsed: MutableStateFlow<Int> = MutableStateFlow(0)
    var timeElapsed: StateFlow<Int> = _timeElapsed

    private var _currentlyPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var currentlyPlaying: StateFlow<Boolean> = _currentlyPlaying

    private var _playbackButtonIcon: MutableStateFlow<Int> = MutableStateFlow(R.drawable.baseline_play_arrow_36)
    var playbackButtonIcon: StateFlow<Int> = _playbackButtonIcon


    var awaitingResponse: StateFlow<Boolean> = controller.awaitingResponse


    fun setSelectedSpeaker(speaker: String?) {

        _selectedSpeaker.value = speaker
    }

    fun increaseRecordedSeconds() {

        _secondsRecorded.value++
    }

    fun getElapsedTime() {

        _timeElapsed.value = controller.getPlayerElapsedMs()
    }

    fun setCurrentRecording(recording: Recording) {

        _currentRecording.value = recording
        controller.initPlayer(recording)
        _currentRecordingDuration.value = controller.getRecordingDurationInMs()
        _currentlyPlaying.value = false

        _playbackButtonIcon.value = R.drawable.baseline_play_arrow_36
    }

    private fun unsetCurrentRecording() {

        _currentRecording.value = null
        controller.destroyPlayer()
        _currentRecordingDuration.value = 0
        _currentlyPlaying.value = false

        _playbackButtonIcon.value = R.drawable.baseline_play_arrow_36
    }

    fun startPlayback() {

        controller.startPlayback()
        _currentlyPlaying.value = true
        _playbackButtonIcon.value = R.drawable.baseline_pause_36
    }

    fun pausePlayback() {

        controller.pausePlayback()
        _currentlyPlaying.value = false
        _playbackButtonIcon.value = R.drawable.baseline_play_arrow_36
        _timeElapsed.value = controller.getPlayerElapsedMs()
    }

    fun resetPlayback() {

        controller.resetPlayback()
        _currentlyPlaying.value = false
        _playbackButtonIcon.value = R.drawable.baseline_play_arrow_36
        _timeElapsed.value = 0
    }

    fun seekPlayback(ms: Int) {

        controller.seekPlayback(ms)
        _timeElapsed.value = ms
    }

    fun startRecording() {

        viewModelScope.launch(Dispatchers.IO) {

            unsetCurrentRecording()

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

            setCurrentRecording(newRecording)

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

            unsetCurrentRecording()

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

                setCurrentRecording(newRecording)
            }
        }
    }

    fun deleteCurrentRecording() {

        viewModelScope.launch(Dispatchers.IO) {

            controller.deleteRecording(_currentRecording.value!!.id!!)

            unsetCurrentRecording()
        }
    }


    fun sendForConversion(recording: Recording, targetSpeakerClass: SpeakerClass, onSuccess: () -> Unit, onNetworkFailure: (String) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            controller.conversionAPICall(recording.filename, targetSpeakerClass, onSuccess, onNetworkFailure)
        }
    }
}