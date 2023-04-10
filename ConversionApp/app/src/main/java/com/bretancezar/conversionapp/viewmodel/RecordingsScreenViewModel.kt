package com.bretancezar.conversionapp.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
class RecordingsScreenViewModel @Inject constructor (
    private val controller: AppController
): ViewModel() {

    val speakersList: List<SpeakerClass> = SpeakerClass.values().toList()

    private var _selectedSpeaker: MutableStateFlow<SpeakerClass> = MutableStateFlow(SpeakerClass.ORIGINAL)
    private var _currentRecordingsList: MutableStateFlow<LiveData<List<Recording>>> = MutableStateFlow(MutableLiveData(listOf()))

    var selectedSpeaker: StateFlow<SpeakerClass?> = _selectedSpeaker
    var currentRecordingsList: StateFlow<LiveData<List<Recording>>> = _currentRecordingsList

    private var _entityToConfirmDeletion: MutableStateFlow<Recording?> = MutableStateFlow(null)
    var entityToConfirmDeletion: StateFlow<Recording?> = _entityToConfirmDeletion

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

            controller.deleteRecording(_entityToConfirmDeletion.value!!.id!!)
        }
    }

    fun setEntityToConfirm(recording: Recording) {

        viewModelScope.launch(Dispatchers.IO) {

            _entityToConfirmDeletion.value = recording
        }
    }

    fun unsetEntityToConfirm() {

        viewModelScope.launch(Dispatchers.IO) {

            _entityToConfirmDeletion.value = null
        }
    }
}