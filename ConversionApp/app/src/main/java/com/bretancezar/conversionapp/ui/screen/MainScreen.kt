package com.bretancezar.conversionapp.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.bretancezar.conversionapp.R
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.navigation.NavControllerAccessObject
import com.bretancezar.conversionapp.ui.theme.DarkYellow
import com.bretancezar.conversionapp.ui.theme.Purple700
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
    val conversionDialogShown by viewModel.conversionDialogShown.collectAsState()
    val deletionDialogShown by viewModel.deletionDialogShown.collectAsState()

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

            Row {

                RecordingsFab(viewModel, navControllerAccessObject)
            }
        }

    ) {

        MainBody {

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                RecordBtn(viewModel)

                if (currentRecording != null) {

                    FilenameModifier(viewModel, showSnackbarWithMessage)
                    AudioPlayer(viewModel)

                    if (currentRecording!!.speakerClass == SpeakerClass.ORIGINAL) {

                        ConversionBtn(viewModel)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround) {

                        DeleteBtn(viewModel)
                        ShareBtn(viewModel)
                    }
                }
            }
        }

        if (conversionDialogShown) {

            AlertDialog(
                onDismissRequest = {
                    viewModel.hideConversionDialog()
                },
                title = {
                    Text("Select Target Speaker")
                },
                text = {
                    SpeakerSelector(viewModel)
                },
                buttons = {

                    val selectedSpeakerClass by viewModel.selectedSpeaker.collectAsState()
                    val awaitingResponse by viewModel.awaitingResponse.collectAsState()

                    Row(
                        modifier = Modifier.padding(all = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(

                            modifier = Modifier.fillMaxWidth(fraction = 0.4f),
                            onClick = {

                                if (selectedSpeakerClass != null && !awaitingResponse) {

                                    viewModel.sendForConversion(
                                        currentRecording!!,
                                        selectedSpeakerClass!!, {

                                            viewModel.hideConversionDialog()
                                            navControllerAccessObject.navigateFromMainToRecordings(selectedSpeakerClass!!)
                                        }, {

                                            viewModel.hideConversionDialog()
                                            showSnackbarWithMessage("Network Error.")
                                        }
                                    )
                                }
                            }
                        ) {

                            if (!awaitingResponse) {
                                Text("Send")
                            }
                            else {
                                Text("Waiting...")
                            }
                        }

                        if (!awaitingResponse) {
                            Button(
                                modifier = Modifier.fillMaxWidth(fraction = 0.66f),
                                onClick = {
                                    viewModel.hideConversionDialog()
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            )
        }

        if (deletionDialogShown) {

            AlertDialog(
                onDismissRequest = {
                    viewModel.hideDeletionDialog()
                },
                title = {
                    Text(text = "Confirmation")
                },
                text = {

                    Text("Are you sure you want to delete this recording?")
                },
                buttons = {
                    Row(
                        modifier = Modifier.padding(all = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(

                            modifier = Modifier.fillMaxWidth(fraction = 0.4f),
                            onClick = {

                                viewModel.deleteCurrentRecording()
                                viewModel.hideDeletionDialog()
                            }
                        ) {
                            Text("Yes")
                        }
                        Button(

                            modifier = Modifier.fillMaxWidth(fraction = 0.66f),
                            onClick = {

                                viewModel.hideDeletionDialog()
                            }
                        ) {
                            Text("No")
                        }
                    }
                }
            )
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
fun RecordBtn(viewModel: MainScreenViewModel) {

    val buttonState: Boolean by viewModel.recordingButtonState.collectAsState()

    val buttonColor by viewModel.buttonColor.collectAsState()

    val buttonIcon by viewModel.buttonIcon.collectAsState()

    val recordedSeconds by viewModel.secondsRecorded.collectAsState()

    LaunchedEffect(buttonState) {

        while (buttonState && recordedSeconds < viewModel.maxRecordingSeconds-1) {
            delay(1.seconds)
            viewModel.increaseRecordedSeconds()
        }

            if (recordedSeconds == viewModel.maxRecordingSeconds-1) {

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
                contentDescription = "toggleRecording"
            )
        }
    }

    Text(text = "${deltaSecondsToTime(recordedSeconds)} / ${deltaSecondsToTime(viewModel.maxRecordingSeconds)}",
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

@Composable
fun AudioPlayer(viewModel: MainScreenViewModel) {

    val timeElapsed by viewModel.timeElapsed.collectAsState()

    val duration by viewModel.currentRecordingDuration.collectAsState()

    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()

    val buttonIcon by viewModel.playbackButtonIcon.collectAsState()

    var sliderIsChanging by remember { mutableStateOf(false) }

    var localSliderValue by remember { mutableStateOf(0f) }

    val sliderProgress =
        if (sliderIsChanging) localSliderValue else timeElapsed.toFloat() / duration

    LaunchedEffect(currentlyPlaying) {

        while (currentlyPlaying && timeElapsed < duration) {
            delay(0.1.seconds)
            viewModel.getElapsedTime()
        }
        if (timeElapsed >= duration) {
            viewModel.resetPlayback()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.DarkGray,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {

        Text(text = deltaSecondsToTime(timeElapsed / 1000),
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp)

        Slider(
            value = sliderProgress,
            modifier = Modifier.fillMaxWidth(fraction = 0.6f),
            colors = SliderDefaults.colors(Color.White),
            onValueChange = { newPosition -> run {
                    localSliderValue = newPosition
                    sliderIsChanging = true
                }
            },
            onValueChangeFinished = {
                viewModel.seekPlayback((duration * localSliderValue).toInt())
                sliderIsChanging = false
            }
        )

        Text(text = deltaSecondsToTime(duration / 1000),
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp)

        Button(
            onClick = {
                if (currentlyPlaying) {
                    viewModel.pausePlayback()
                }
                else {
                    viewModel.startPlayback()
                }
            },
            modifier = Modifier
                .height(intrinsicSize = IntrinsicSize.Max),
            colors = ButtonDefaults.buttonColors(Color(0x00000000))
        ) {

            Image(
                painter = painterResource(buttonIcon),
                contentDescription = "togglePlayback"
            )
        }
    }
}

@Composable
fun ConversionBtn(viewModel: MainScreenViewModel) {

    Button(
        onClick = {

            viewModel.showConversionDialog()
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                color = Purple700
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                shape = RoundedCornerShape(32.dp)
            )
    ) {

        Text(text = "Send for Conversion...",
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp
        )
    }
}

@Composable
fun DeleteBtn(viewModel: MainScreenViewModel) {

    Button(
        onClick = {
            viewModel.showDeletionDialog()
        },
        modifier = Modifier
            .fillMaxWidth(fraction = 0.4f)
            .clip(RoundedCornerShape(32.dp))
            .background(
                color = Color.Red,
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                shape = RoundedCornerShape(32.dp)
            ),
        colors = ButtonDefaults.buttonColors(Color.Red)
    ) {

        Text(text = "Delete",
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp
        )
    }
}

@Composable
fun ShareBtn(viewModel: MainScreenViewModel) {

    Button(
        onClick = {
            viewModel.startShareIntent()
        },
        modifier = Modifier
            .fillMaxWidth(fraction = 0.66f)
            .clip(RoundedCornerShape(32.dp))
            .background(
                color = Color.Blue,
            )
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                shape = RoundedCornerShape(32.dp)
            ),
        colors = ButtonDefaults.buttonColors(Color.Blue)
    ) {

        Text(text = "Share",
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp
        )
    }
}


@Composable
fun SpeakerSelector(viewModel: MainScreenViewModel) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val selectedSpeaker by viewModel.selectedSpeaker.collectAsState()
    val awaitingResponse by viewModel.awaitingResponse.collectAsState()

    Box(
        modifier = Modifier
            .clickable(onClick = {
                if (!awaitingResponse) {
                    menuExpanded = true
                }
            })
            .padding(16.dp, 16.dp, 16.dp, 0.dp),
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
                .padding(8.dp)
                .height(24.dp),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {

            Text(text = selectedSpeaker?.toString() ?: "-- SELECT --")
        }

        DropdownMenu(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.5f)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colors.onBackground),
                    shape = RoundedCornerShape(16.dp)
                ).background(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(16.dp)
                ),
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

                    Text(text = c.toString())
                }
            } }
        }
    }
}