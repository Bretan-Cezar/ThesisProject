package com.bretancezar.conversionapp.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class RecordingsScreenViewModel @Inject constructor (
    private val controller: AppController,
    private val context: Context
): ViewModel() {

    val speakersList: List<SpeakerClass> = SpeakerClass.values().toList()

    private var _selectedSpeaker: MutableStateFlow<SpeakerClass> = MutableStateFlow(SpeakerClass.ORIGINAL)
    var selectedSpeaker: StateFlow<SpeakerClass?> = _selectedSpeaker

    private var _currentRecordingsList: MutableStateFlow<LiveData<List<Recording>>> = MutableStateFlow(MutableLiveData(listOf()))
    var currentRecordingsList: StateFlow<LiveData<List<Recording>>> = _currentRecordingsList

    private var _entityToConfirmForDeletion: MutableStateFlow<Recording?> = MutableStateFlow(null)
    var entityToConfirmForDeletion: StateFlow<Recording?> = _entityToConfirmForDeletion

    init {

        viewModelScope.launch(Dispatchers.IO) {

            _currentRecordingsList.value = controller.findRecordingsBySpeakerClass(SpeakerClass.ORIGINAL)
        }
    }

    fun setRecordingsFromSpeakerClass(speakerClass: SpeakerClass) {

        viewModelScope.launch(Dispatchers.IO) {

            _selectedSpeaker.value = speakerClass
            _currentRecordingsList.value = controller.findRecordingsBySpeakerClass(speakerClass)
        }
    }

    fun deleteRecording() {

        viewModelScope.launch(Dispatchers.IO) {

            controller.deleteRecording(_entityToConfirmForDeletion.value!!.id!!)
        }
    }

    fun setEntityToConfirmForDeletion(recording: Recording) {

        viewModelScope.launch(Dispatchers.IO) {

            _entityToConfirmForDeletion.value = recording
        }
    }

    fun unsetEntityToConfirmForDeletion() {

        viewModelScope.launch(Dispatchers.IO) {

            _entityToConfirmForDeletion.value = null
        }
    }

    fun startShareIntent(recording: Recording) {

        val speakerClass = recording.speakerClass
        val filename = recording.filename
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