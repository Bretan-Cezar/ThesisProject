package com.bretancezar.conversionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import com.bretancezar.conversionapp.navigation.AppNavigation
import com.bretancezar.conversionapp.ui.theme.ConversionAppTheme
import com.bretancezar.conversionapp.utils.checkPermissions
import com.bretancezar.conversionapp.utils.getRequiredPermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (!checkPermissions(this)) {

            ActivityCompat.requestPermissions(this, getRequiredPermissions().toTypedArray(), 0)
        }

        while (!checkPermissions(this)) {}

        setContent {

            ConversionAppTheme {

                MainApplication()
            }
        }

    }
}

@Composable
fun MainApplication() {

    AppNavigation()
}
