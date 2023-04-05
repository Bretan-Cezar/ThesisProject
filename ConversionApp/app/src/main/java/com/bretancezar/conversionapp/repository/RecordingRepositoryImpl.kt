package com.bretancezar.conversionapp.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.db.RecordingDAO
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(private val dao: RecordingDAO) : RecordingRepository {

    override fun findBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>> {

        val recordings = dao.findBySpeakerClass(speakerClass)

        Log.i("REPO", "Successfully retrieved list of recordings by speaker class $speakerClass from local DB.")

        return recordings
    }

    override fun save(recording: Recording) {

        dao.save(recording)
        Log.i("REPO", "Successfully saved new recording entry $recording to local DB.")
    }

    override fun deleteById(id: Long) {

        dao.deleteById(id)
        Log.i("REPO", "Successfully deleted recording entry id=$id from local DB.")
    }
}