package com.bretancezar.conversionapp.repository

import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass

interface RecordingRepository {

    fun findById(id: Long): Recording

    fun findBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>>

    fun save(recording: Recording): Recording

    fun deleteById(id: Long)

    fun updateFilenameById(id: Long, newFilename: String): Recording
}

