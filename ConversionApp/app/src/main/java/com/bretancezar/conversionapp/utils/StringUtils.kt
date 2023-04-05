package com.bretancezar.conversionapp.utils

import androidx.room.ProvidedTypeConverter
import java.time.LocalDateTime

fun LocalDateTime?.formatToReadableDateTime() : String {

    if (this == null || this == LocalDateTime.MIN)
        return ""

    return "${this.year}-${this.monthValue.toString().padStart(2, '0')}-${this.dayOfMonth.toString().padStart(2, '0')} ${this.hour.toString().padStart(2, '0')}:${this.minute.toString().padStart(2, '0')}"
}

