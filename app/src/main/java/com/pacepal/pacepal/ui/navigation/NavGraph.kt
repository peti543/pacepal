package com.pacepal.pacepal.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pacepal.pacepal.ui.history.HistoryScreen
import com.pacepal.pacepal.ui.main.MainScreen
import com.pacepal.pacepal.ui.onboarding.OnboardingScreen
import com.pacepal.pacepal.ui.settings.SettingsScreen
import com.pacepal.pacepal.viewmodel.PacePalViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun PacePalNavGraph(
    navController: NavHostController,
    viewModel: PacePalViewModel = viewModel()
) {
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val drinks by viewModel.drinks.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val showAllClear by viewModel.showAllClear.collectAsState()

    val startDestination = if (isOnboardingComplete) Routes.MAIN else Routes.ONBOARDING

    // Show "All clear" dialog
    if (showAllClear) {
        AllClearDialog(onDismiss = { viewModel.dismissAllClear() })
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = { weight, gender, threshold ->
                    viewModel.completeOnboarding(weight, gender, threshold)
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                sessionState = sessionState,
                funThreshold = profile.funThreshold,
                onDrinkTap = { viewModel.logDrink(it) },
                onCustomDrinkLog = { base, vol, abv -> viewModel.logCustomDrink(base, vol, abv) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onDismissSnackbar = { viewModel.dismissSnackbar() },
                onUndoLastDrink = { viewModel.undoLastDrink() },
                snackbarMessage = snackbarMessage
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                drinks = drinks,
                onClearSession = { viewModel.clearSession() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                profile = profile,
                onProfileUpdate = { viewModel.updateProfile(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
