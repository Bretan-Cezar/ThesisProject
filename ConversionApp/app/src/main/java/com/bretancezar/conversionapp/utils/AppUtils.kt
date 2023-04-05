package com.bretancezar.conversionapp.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import com.bretancezar.conversionapp.domain.SpeakerClass
import java.io.*
import java.time.LocalDateTime


fun getRequiredPermissions(): List<String> {

    // TODO: Platform-wise permissions

    return listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )
}

fun checkPermissions(activity: ComponentActivity): Boolean {

    for (permission in getRequiredPermissions()) {

        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }

    return true
}
