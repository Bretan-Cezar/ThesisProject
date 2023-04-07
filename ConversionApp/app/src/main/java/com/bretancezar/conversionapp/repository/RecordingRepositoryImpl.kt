package com.bretancezar.conversionapp.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.db.RecordingDAO
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(private val dao: RecordingDAO) : RecordingRepository {

    override fun findById(id: Long): Recording {

        val recording = dao.findById(id)

        if (recording == null) {
            Log.wtf("REPO", "No recording with the given id was found.")
            throw IllegalArgumentException("No recording with the given id was found.")
        }

        return recording
    }

    override fun findBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>> {

        val recordings = dao.findBySpeakerClass(speakerClass)

        Log.i("REPO", "Successfully retrieved list of recordings by speaker class $speakerClass from local DB.")

        return recordings
    }

    override fun save(recording: Recording): Recording {

        val ret = dao.save(recording)
        Log.i("REPO", "Successfully saved new recording entry $recording to local DB.")

        return ret
    }

    override fun deleteById(id: Long) {

        dao.deleteById(id)
        Log.i("REPO", "Successfully deleted recording entry id=$id from local DB.")
    }

    override fun updateFilenameById(id: Long, newFilename: String) {

        dao.updateFilename(id, newFilename)
        Log.i("REPO", "Successfully updated filename to $newFilename for recording entry id=$id")
    }
}