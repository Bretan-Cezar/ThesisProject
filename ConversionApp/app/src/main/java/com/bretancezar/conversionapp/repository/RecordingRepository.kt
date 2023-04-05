package com.bretancezar.conversionapp.repository

import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass

interface RecordingRepository {

    fun findBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>>

    fun save(recording: Recording)

    fun deleteById(id: Long)
}

