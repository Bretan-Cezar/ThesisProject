package com.bretancezar.conversionapp.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bretancezar.conversionapp.utils.Converters
import java.time.LocalDateTime

@Entity(tableName = "recording")
@TypeConverters(Converters::class)
data class Recording(

    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long? = null,
    @ColumnInfo(name = "datetime") val datetime: LocalDateTime,
    @ColumnInfo(name = "speaker_class") val speakerClass: SpeakerClass,
    @ColumnInfo(name = "filename") val filename: String
)