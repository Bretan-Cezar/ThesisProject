package com.bretancezar.conversionapp.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.ui.screen.MainScreen
import com.bretancezar.conversionapp.ui.screen.RecordingsScreen
import com.bretancezar.conversionapp.viewmodel.MainScreenViewModel
import com.bretancezar.conversionapp.viewmodel.RecordingsScreenViewModel

class NavControllerAccessObject(
    navController: NavHostController,
    mainScreenViewModel: MainScreenViewModel,
    recordingsScreenViewModel: RecordingsScreenViewModel
) {

    private val controller = navController
    private val mainVM = mainScreenViewModel
    private val recVM = recordingsScreenViewModel

    fun navigateFromMainToRecordings(selectedClass: SpeakerClass = SpeakerClass.ORIGINAL) {

        recVM.setRecordingsFromSpeakerClass(selectedClass)
        controller.navigate("recordings")
    }

    fun navigateToMainAndViewRecording(recording: Recording) {

        mainVM.setCurrentRecording(recording)
        controller.navigate("main") {
            popUpTo("recordings") {inclusive = true}
        }
    }
}

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    val recordingsScreenViewModel: RecordingsScreenViewModel = hiltViewModel()

    val accessObject = NavControllerAccessObject(
        navController,
        mainScreenViewModel,
        recordingsScreenViewModel
    )

    NavHost(navController = navController, startDestination = "main") {

        composable(
            route = "main"
        ) {

            MainScreen(
                navControllerAccessObject = accessObject,
                viewModel = mainScreenViewModel
            )
        }

        composable(
            route = "recordings"
        ) {

            RecordingsScreen(
                navControllerAccessObject = accessObject,
                viewModel = recordingsScreenViewModel
            )
        }
    }
}