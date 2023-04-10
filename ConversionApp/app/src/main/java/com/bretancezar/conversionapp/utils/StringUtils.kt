package com.bretancezar.conversionapp.utils

import androidx.room.ProvidedTypeConverter
import java.time.LocalDateTime

fun LocalDateTime?.formatToReadableDateTime(): String {

    if (this == null || this == LocalDateTime.MIN)
        return ""

    return "${this.year}-${this.monthValue.toString().padStart(2, '0')}-${this.dayOfMonth.toString().padStart(2, '0')} ${this.hour.toString().padStart(2, '0')}:${this.minute.toString().padStart(2, '0')}"
}

fun LocalDateTime?.formatToFileDateTime(): String {

    if (this == null || this == LocalDateTime.MIN)
        return ""

    return "${this.year}-${this.monthValue.toString().padStart(2, '0')}-${this.dayOfMonth.toString().padStart(2, '0')}T${this.hour.toString().padStart(2, '0')}.${this.minute.toString().padStart(2, '0')}"

}

fun deltaSecondsToTime(sec: Int): String {

    return "${sec/60}:${(sec%60).toString().padStart(2, '0')}"
}

fun getFilenameWithoutExtension(filename: String): String {

    val split = filename.split('.')

    return split.subList(0, split.size-1).reduce { acc, s -> acc + s }
}

fun getFileExtension(filename: String): AudioFileFormats {

    val split = filename.split('.')

    return AudioFileFormats.valueOf(split.last().toString().uppercase())
}

fun checkFilenameHasExtension(filename: String, ext: AudioFileFormats): Boolean {

    return if (ext == AudioFileFormats.WAV) {

        filename.slice(IntRange(filename.length-4, filename.length-1)) == ".wav"
    }
    else {
        filename.slice(IntRange(filename.length-5, filename.length-1)) == ".flac"
    }
}