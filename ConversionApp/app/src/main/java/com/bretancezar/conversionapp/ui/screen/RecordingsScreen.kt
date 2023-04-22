package com.bretancezar.conversionapp.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.R
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.navigation.NavControllerAccessObject
import com.bretancezar.conversionapp.ui.theme.DarkYellow
import com.bretancezar.conversionapp.utils.formatToReadableDateTime
import com.bretancezar.conversionapp.viewmodel.RecordingsScreenViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun RecordingsScreen(
    navControllerAccessObject: NavControllerAccessObject,
    viewModel: RecordingsScreenViewModel
) {

    val scaffoldState = rememberScaffoldState()

    val entityToConfirm by viewModel.entityToConfirmDeletion.collectAsState()

    Scaffold(

        topBar = { RecordingsToolbar() },
        scaffoldState = scaffoldState,

    ) {

        Column {

            SpeakerSelector(viewModel)
            RecordingsList(viewModel, navControllerAccessObject)
        }

        if (entityToConfirm != null) {

            AlertDialog(
                onDismissRequest = {
                    viewModel.unsetEntityToConfirm()
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

                                viewModel.deleteRecording()
                                viewModel.unsetEntityToConfirm()
                            }
                        ) {
                            Text("Yes")
                        }
                        Button(

                            modifier = Modifier.fillMaxWidth(fraction = 0.66f),
                            onClick = {

                                viewModel.unsetEntityToConfirm()
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

@Composable
fun SpeakerSelector(viewModel: RecordingsScreenViewModel) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val selectedSpeaker by viewModel.selectedSpeaker.collectAsState()

    Box(
        modifier = Modifier
            .clickable(onClick = {

                menuExpanded = true

            })
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
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
                .height(50.dp),

            horizontalArrangement = Arrangement.Center) {

            selectedSpeaker.let {
                if (selectedSpeaker != null) {
                    Text(it.toString(), Modifier.align(Alignment.CenterVertically))
                }
                else {
                    Text("-- SELECT --")
                }
            }
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
                onClick = { viewModel.setRecordingsFromSpeakerClass(c); menuExpanded = false }
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

@Composable
fun RecordingsList(
    viewModel: RecordingsScreenViewModel,
    navControllerAccessObject: NavControllerAccessObject
) {

    val recordingsList by viewModel.currentRecordingsList.collectAsState()
    val recordingsListState by recordingsList.observeAsState()

    Surface(

        modifier = Modifier.fillMaxWidth()
    ) {

        LazyColumn(

            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(recordingsListState ?: listOf()) {

                RecordingCard(viewModel, it, navControllerAccessObject)
            }

        }
    }
}

@Composable
fun RecordingCard(
    viewModel: RecordingsScreenViewModel,
    entity: Recording,
    navControllerAccessObject: NavControllerAccessObject,
) {

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
            .height(IntrinsicSize.Min),

        verticalAlignment = Alignment.CenterVertically

    ) {

        Column(

            modifier = Modifier
                .padding(20.dp, 20.dp, 0.dp, 20.dp)
                .fillMaxWidth(fraction = 0.66f),

            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(text = "Name: " + entity.filename)
            Text(text = "Datetime: " + entity.datetime.formatToReadableDateTime())
        }

        RecordingCardButtonDivider()

        ViewRecordingButton(entity, navControllerAccessObject)

        RecordingCardButtonDivider()

        ShareRecordingButton(entity)

        RecordingCardButtonDivider()

        DeleteRecordingButton(viewModel, entity)
    }
}

@Composable
fun ViewRecordingButton(entity: Recording, navControllerAccessObject: NavControllerAccessObject) {

    Column(

        modifier = Modifier
            .fillMaxWidth(fraction = 0.33f)
            .fillMaxHeight()
            .clickable {
                navControllerAccessObject.navigateToMainAndViewRecording(entity)
            }
            .background(
                color = DarkYellow),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.baseline_edit_24),
            contentDescription = "view"
        )
    }
}

@Composable
fun ShareRecordingButton(entity: Recording) {

    Column(

        modifier = Modifier
            .fillMaxWidth(fraction = 0.5f)
            .fillMaxHeight()
            .clickable {
                       // TODO open share toolbar
            }
            .background(Color.Blue),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.baseline_share_24),
            contentDescription = "share"
        )
    }
}

@Composable
fun DeleteRecordingButton(viewModel: RecordingsScreenViewModel, entity: Recording) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable {
                viewModel.setEntityToConfirm(entity)
            }
            .background(
                color = Color.Red,
                shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp)),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.baseline_clear_24),
            contentDescription = "delete"
        )
    }
}

@Composable
fun RecordingCardButtonDivider() {
    Divider(
        modifier = Modifier
            .fillMaxHeight()
            .width(2.dp),
        color = MaterialTheme.colors.onBackground
    )
}