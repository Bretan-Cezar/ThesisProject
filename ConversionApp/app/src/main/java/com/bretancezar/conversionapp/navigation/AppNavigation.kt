package com.bretancezar.conversionapp.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.bretancezar.conversionapp.ui.screen.MainScreen
import com.bretancezar.conversionapp.ui.screen.RecordingsScreen
import com.bretancezar.conversionapp.viewmodel.MainScreenViewModel
import com.bretancezar.conversionapp.viewmodel.RecordingsScreenViewModel

class NavControllerAccessObject(navController: NavHostController) {

    val controller = navController

    fun navigateFromMainToRecordings() {

        controller.navigate("recordings")
    }
}

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val accessObject = NavControllerAccessObject(navController)

    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    val recordingsScreenViewModel: RecordingsScreenViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "main") {

        composable(
            route = "main"
        ) {

            MainScreen(navControllerAccessObject = accessObject, viewModel = mainScreenViewModel)
        }

        composable(
            route = "recordings"
        ) {

            RecordingsScreen(navControllerAccessObject = accessObject, viewModel = recordingsScreenViewModel)
        }
    }
}