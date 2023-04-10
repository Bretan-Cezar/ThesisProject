package com.bretancezar.conversionapp.utils

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.bretancezar.conversionapp.domain.SpeakerClass
import java.time.LocalDateTime

@ProvidedTypeConverter
class Converters {

    @TypeConverter
    fun formatToReadableDateTime(dateTime: LocalDateTime?): String {

        if (dateTime == null || dateTime == LocalDateTime.MIN)
            return ""

        return "${
            dateTime.year
        }-${
            dateTime.monthValue.toString().padStart(2, '0')
        }-${
            dateTime.dayOfMonth.toString().padStart(2, '0')
        }T${dateTime.hour.toString().padStart(2, '0')
        }:${dateTime.minute.toString().padStart(2, '0')
        }:00Z"
    }

    @TypeConverter
    fun parseReadableDateTime(string: String?): LocalDateTime? {

        if (string == null || string == "")
            return null

        val year = string.substring(0, 4).toInt()

        val month = string.substring(5, 7).trimStart { it == '0' }.toInt()

        val day = string.substring(8, 10).trimStart { it == '0' }.toInt()

        val rawHour: String = string.substring(11, 13).trimStart { it == '0' }
        val hour: Int = if (rawHour == "") 0 else rawHour.toInt()

        val rawMinute = string.substring(14, 16).trimStart { it == '0' }
        val minute: Int = if (rawMinute == "") 0 else rawMinute.toInt()

        return LocalDateTime.of(year, month, day, hour, minute)
    }

    @TypeConverter
    fun classEnumToString(speakerClass: SpeakerClass): String {
        return speakerClass.toString()
    }

    @TypeConverter
    fun stringToClassEnum(string: String): SpeakerClass {
        return SpeakerClass.valueOf(string)
    }
}