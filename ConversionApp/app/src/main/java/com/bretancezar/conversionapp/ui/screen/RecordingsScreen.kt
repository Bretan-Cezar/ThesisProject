package com.bretancezar.conversionapp.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bretancezar.conversionapp.navigation.NavControllerAccessObject
import com.bretancezar.conversionapp.viewmodel.MainScreenViewModel
import com.bretancezar.conversionapp.viewmodel.RecordingsScreenViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun RecordingsScreen(
    navControllerAccessObject: NavControllerAccessObject,
    viewModel: RecordingsScreenViewModel
) {

    val scaffoldState = rememberScaffoldState()

    Scaffold(

        topBar = { RecordingsToolbar() },
        scaffoldState = scaffoldState,

    ) {

    }
}

@Composable
fun RecordingsToolbar() {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        TopAppBar(
            title = { Text(text = "Saved Recordings") },
            backgroundColor = MaterialTheme.colors.primaryVariant,
            contentColor = MaterialTheme.colors.onPrimary
        )
    }
}