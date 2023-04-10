package com.bretancezar.conversionapp.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bretancezar.conversionapp.R
import com.bretancezar.conversionapp.navigation.NavControllerAccessObject
import com.bretancezar.conversionapp.ui.theme.DarkYellow
import com.bretancezar.conversionapp.ui.theme.Typography
import com.bretancezar.conversionapp.utils.deltaSecondsToTime
import com.bretancezar.conversionapp.viewmodel.MainScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainScreen(
    navControllerAccessObject: NavControllerAccessObject,
    viewModel: MainScreenViewModel
) {

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val currentRecording by viewModel.currentRecording.collectAsState()

    val showSnackbarWithMessage: (String) -> Unit = {

        coroutineScope.launch {

            val snackBarResult = scaffoldState.snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss"
            )

            when (snackBarResult) {
                SnackbarResult.Dismissed -> Log.d("MainSnackBar", "Dismissed")
                SnackbarResult.ActionPerformed -> Log.d("MainSnackBar", "Dismissed")
            }
        }
    }

    Scaffold(

        topBar = { MainToolbar() },
        scaffoldState = scaffoldState,

        floatingActionButton = {

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                RecordingsFab(viewModel, navControllerAccessObject)
            }
        }

    ) {

        MainBody {

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                RecordBtn(viewModel)

                if (currentRecording != null) {

                    FilenameModifier(viewModel, showSnackbarWithMessage)
                }
            }
        }
    }
}

@Composable
fun MainToolbar() {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        TopAppBar(
            title = { Text(text = "Voice Recording & Conversion") },
            backgroundColor = MaterialTheme.colors.primaryVariant,
            contentColor = MaterialTheme.colors.onPrimary
        )
    }
}

@Composable
fun MainBody(content: @Composable () -> Unit) {

    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier
            .fillMaxWidth()

    ) {

        content()
    }
}


@Composable
fun SpeakerSelector(viewModel: MainScreenViewModel) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val selectedSpeaker by viewModel.selectedSpeaker.collectAsState()

    Box(
        modifier = Modifier
            .clickable(onClick = {

                menuExpanded = true

            }),
        contentAlignment = Alignment.Center) {

        Row(
            modifier = Modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(16.dp)
            )
            .fillMaxWidth()
            .padding(16.dp)
            .height(24.dp),

            horizontalArrangement = Arrangement.Center) {

            Text(text = selectedSpeaker ?: "-- SELECT --")
        }

        DropdownMenu(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                    shape = RoundedCornerShape(16.dp)
                ).background(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(16.dp)
                ),
            offset = DpOffset(0.dp, 0.dp),
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }

        ) {

            DropdownMenuItem(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { viewModel.setSelectedSpeaker(null); menuExpanded = false }
            ) {

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                ) {

                    Text(text = "-- SELECT --")
                }
            }

            viewModel.speakersList.forEach {

                c -> DropdownMenuItem(

                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .drawBehind {
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1f
                                )
                                    },
                    onClick = { viewModel.setSelectedSpeaker(c); menuExpanded = false }
                ) {

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                    ) {

                        Text(text = c)
                    }
            } }
        }
    }
}

@Composable
fun RecordBtn(viewModel: MainScreenViewModel) {

    val buttonState: Boolean by viewModel.recordingButtonState.collectAsState()

    val buttonColor by viewModel.buttonColor.collectAsState()

    val buttonIcon by viewModel.buttonIcon.collectAsState()

    val recordedSeconds by viewModel.secondsRecorded.collectAsState()

    LaunchedEffect(buttonState) {

        while (buttonState && recordedSeconds < 300) {
            delay(1.seconds)
            viewModel.increaseTimer()
        }

        if (recordedSeconds == 300) {

            viewModel.stopAndSaveRecording()
        }
    }

    Box(
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center) {

        Button(
            onClick = {

                    if (!buttonState) {

                        viewModel.startRecording()

                    } else {

                        viewModel.stopAndSaveRecording()
                    }

            },
            modifier = Modifier.height(160.dp).width(160.dp),
            colors = ButtonDefaults.buttonColors(buttonColor),
            shape = CircleShape
        ) {

            Image(
                painter = painterResource(buttonIcon),
                contentDescription = "recordBtnState"
            )
        }
    }

    Text(text = "${deltaSecondsToTime(recordedSeconds)} / ${deltaSecondsToTime(300)}",
        modifier = Modifier,
        fontSize = 24.sp)
}

@Composable
fun RecordingsFab(viewModel: MainScreenViewModel, navControllerAccessObject: NavControllerAccessObject) {

    FloatingActionButton(
        shape = CircleShape,
        onClick = {
            viewModel.stopAndAbortRecording()
            navControllerAccessObject.navigateFromMainToRecordings()
        },
        backgroundColor = MaterialTheme.colors.background,
    ) {

        Image(
            painter = painterResource(R.drawable.baseline_history_64),
            contentDescription = "recordings",
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
fun FilenameModifier(viewModel: MainScreenViewModel, snackbarAction: (String) -> Unit) {

    val recording by viewModel.currentRecording.collectAsState()

    var fieldText: String by remember {
        mutableStateOf(recording?.filename ?: "")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(DarkYellow)
    ) {

        TextField(
            value = fieldText,
            onValueChange = {
                fieldText = it
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(fraction = 0.7f)
                .horizontalScroll(rememberScrollState())
                .background(Color.DarkGray)
        )

        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = DarkYellow),

            onClick = {

                viewModel.renameCurrentRecording(
                    fieldText,
                    {snackbarAction("Recording successfully renamed!")},
                    snackbarAction
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Max)
                .background(
                    color = DarkYellow
                ),
        ) {

            Text(text = "Rename")
        }

    }
}