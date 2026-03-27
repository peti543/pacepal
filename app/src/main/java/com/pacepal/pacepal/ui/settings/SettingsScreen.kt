package com.pacepal.pacepal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepal.pacepal.data.Gender
import com.pacepal.pacepal.data.UserProfile
import com.pacepal.pacepal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    onProfileUpdate: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    var weightText by remember(profile) { mutableStateOf(profile.weightKg.toString()) }
    var gender by remember(profile) { mutableStateOf(profile.gender) }
    var threshold by remember(profile) { mutableDoubleStateOf(profile.funThreshold) }
    var showDisclaimer by remember { mutableStateOf(false) }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Disclaimer", color = AmberPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "PacePal provides estimates only. BAC calculations are approximate and " +
                            "should not be used to determine if you are fit to drive or operate " +
                            "machinery. Always drink responsibly and never drink and drive.",
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimer = false }) {
                    Text("OK", color = AmberPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    fun saveChanges() {
        val weight = weightText.toIntOrNull() ?: profile.weightKg
        onProfileUpdate(UserProfile(weight, gender, threshold))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = {
                        saveChanges()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Gender
            Text("Gender", fontSize = 14.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = gender == Gender.MALE,
                    onClick = {
                        gender = Gender.MALE
                        saveChanges()
                    },
                    label = { Text("Male") },
                    leadingIcon = {
                        Icon(Icons.Default.Male, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPrimary,
                        selectedLabelColor = OnAmber,
                        selectedLeadingIconColor = OnAmber
                    )
                )
                FilterChip(
                    selected = gender == Gender.FEMALE,
                    onClick = {
                        gender = Gender.FEMALE
                        saveChanges()
                    },
                    label = { Text("Female") },
                    leadingIcon = {
                        Icon(Icons.Default.Female, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPrimary,
                        selectedLabelColor = OnAmber,
                        selectedLeadingIconColor = OnAmber
                    )
                )
            }

            // Weight
            Text("Weight", fontSize = 14.sp, color = TextSecondary)
            OutlinedTextField(
                value = weightText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                        weightText = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                suffix = { Text("kg", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = AmberPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Fun Threshold
            Text("Fun Threshold", fontSize = 14.sp, color = TextSecondary)
            Column {
                Text(
                    text = String.format("%.2f%%", threshold),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
                Slider(
                    value = ((threshold - 0.03) / 0.01).toFloat(),
                    onValueChange = { step ->
                        threshold = 0.03 + (step.toInt() * 0.01)
                    },
                    onValueChangeFinished = { saveChanges() },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AmberPrimary,
                        activeTrackColor = AmberPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.03%", fontSize = 12.sp, color = TextSecondary)
                    Text("0.08%", fontSize = 12.sp, color = TextSecondary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Disclaimer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDisclaimer = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Disclaimer",
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("⚠\uFE0F", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
