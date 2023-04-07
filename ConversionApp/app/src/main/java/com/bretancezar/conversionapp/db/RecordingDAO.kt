package com.bretancezar.conversionapp.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.utils.Converters

@Dao
@TypeConverters( Converters::class )
interface RecordingDAO {

    @Query("SELECT * FROM recording WHERE id = :id")
    fun findById(id: Long): Recording?

    @Query("SELECT * FROM recording WHERE speaker_class = :speakerClass")
    fun findBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>>

    @Insert
    fun save(recording: Recording): Recording

    @Query("DELETE FROM recording WHERE id = :id")
    fun deleteById(id: Long)

    @Query("UPDATE recording SET filename = :newFilename WHERE id = :id")
    fun updateFilename(id: Long, newFilename: String)
}