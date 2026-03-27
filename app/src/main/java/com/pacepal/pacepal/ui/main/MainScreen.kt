package com.pacepal.pacepal.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepal.pacepal.data.*
import com.pacepal.pacepal.ui.components.BacPanel
import com.pacepal.pacepal.ui.components.CustomDrinkDialog
import com.pacepal.pacepal.ui.components.DrinkCard
import com.pacepal.pacepal.ui.theme.*

@Composable
fun MainScreen(
    sessionState: SessionState,
    funThreshold: Double,
    onDrinkTap: (DrinkOption) -> Unit,
    onCustomDrinkLog: (DrinkOption, Int, Double) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismissSnackbar: () -> Unit,
    onUndoLastDrink: () -> Unit,
    snackbarMessage: String?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customDrinkBase by remember { mutableStateOf<DrinkOption?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            val result = snackbarHostState.showSnackbar(
                message = snackbarMessage,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoLastDrink()
            }
            onDismissSnackbar()
        }
    }

    // Custom drink dialog
    customDrinkBase?.let { baseDrink ->
        CustomDrinkDialog(
            baseDrink = baseDrink,
            onConfirm = { vol, abv ->
                onCustomDrinkLog(baseDrink, vol, abv)
                customDrinkBase = null
            },
            onDismiss = { customDrinkBase = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PacePal",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
                Row {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            BacPanel(
                currentBac = sessionState.currentBac,
                funThreshold = funThreshold,
                nextSafeDrinkMinutes = sessionState.nextSafeDrinkMinutes,
                fullySoberMinutes = sessionState.fullySoberMinutes,
                trend = sessionState.bacTrend
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = AmberPrimary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("\uD83C\uDF7A Beer") },
                    selectedContentColor = AmberPrimary,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("\uD83E\uDD43 Spirit") },
                    selectedContentColor = AmberPrimary,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("\uD83C\uDF77 Wine") },
                    selectedContentColor = AmberPrimary,
                    unselectedContentColor = TextSecondary
                )
            }

            val drinks = when (selectedTab) {
                0 -> DrinkOptions.beers
                1 -> DrinkOptions.spirits
                else -> DrinkOptions.wines
            }

            val isAboveThreshold = sessionState.currentBac > funThreshold

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(drinks, key = { it.name + it.volumeMl + it.abvPercent }) { drink ->
                    DrinkCard(
                        drink = drink,
                        isAboveThreshold = isAboveThreshold,
                        onTap = { onDrinkTap(drink) },
                        onLongPress = { customDrinkBase = drink }
                    )
                }
            }
        }
    }
}
