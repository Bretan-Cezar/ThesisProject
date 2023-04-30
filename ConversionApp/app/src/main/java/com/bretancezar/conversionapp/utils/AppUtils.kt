package com.bretancezar.conversionapp.utils

import android.content.pm.PackageManager
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat


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
