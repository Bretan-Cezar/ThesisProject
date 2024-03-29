package com.bretancezar.conversionapp.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bretancezar.conversionapp.R
import com.bretancezar.conversionapp.controller.AppController
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.utils.getFileExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainScreenViewModel @Inject constructor (
    private val controller: AppController,
    private val context: Context
): ViewModel() {

    val speakersList: List<SpeakerClass> =
        SpeakerClass
            .values()
            .toList()
            .filter { return@filter it != SpeakerClass.ORIGINAL }


    private var _selectedSpeaker: MutableStateFlow<SpeakerClass?> = MutableStateFlow(null)
    var selectedSpeaker: StateFlow<SpeakerClass?> = _selectedSpeaker

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

    private var _conversionDialogShown: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var conversionDialogShown: StateFlow<Boolean> = _conversionDialogShown

    private var _deletionDialogShown: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var deletionDialogShown: StateFlow<Boolean> = _deletionDialogShown

    var awaitingResponse: StateFlow<Boolean> = controller.awaitingResponse

    val maxRecordingSeconds = 120

    fun setSelectedSpeaker(speaker: SpeakerClass?) {

        _selectedSpeaker.value = speaker
    }

    fun increaseRecordedSeconds() {

        _secondsRecorded.value++
    }

    fun getElapsedTime() {

        _timeElapsed.value = controller.getPlayerElapsedMs()
    }

    fun showConversionDialog() {

        _conversionDialogShown.value = true
    }

    fun hideConversionDialog() {

        _conversionDialogShown.value = false
    }

    fun showDeletionDialog() {

        _deletionDialogShown.value = true
    }

    fun hideDeletionDialog() {

        _deletionDialogShown.value = false
    }

    fun startShareIntent() {

        if (_currentRecording.value != null) {

            val speakerClass = _currentRecording.value!!.speakerClass
            val filename = _currentRecording.value!!.filename
            val file = controller.getRecordingFile(speakerClass, filename)

            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("audio/${getFileExtension(filename)}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)

            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(intent)
        }
    }

    fun setCurrentRecording(recording: Recording) {

        controller.initPlayer(recording)
        _currentRecordingDuration.value = controller.getRecordingDurationMs()
        _currentlyPlaying.value = false

        _playbackButtonIcon.value = R.drawable.baseline_play_arrow_36

        _currentRecording.value = recording
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
            controller.abortRecording()

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

    fun sendForConversion(recording: Recording, targetSpeakerClass: SpeakerClass, onSuccess: () -> Unit, onFailure: (String) -> Unit) {

        controller.conversionAPICall(recording.filename, targetSpeakerClass, onSuccess, onFailure, viewModelScope)
    }
}