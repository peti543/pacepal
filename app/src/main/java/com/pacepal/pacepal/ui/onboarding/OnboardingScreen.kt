package com.pacepal.pacepal.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepal.pacepal.data.Gender
import com.pacepal.pacepal.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: (weightKg: Int, gender: Gender, threshold: Double) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var weightKg by remember { mutableStateOf("75") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var threshold by remember { mutableDoubleStateOf(0.05) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step indicators
        Row(
            modifier = Modifier.padding(top = 48.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == step) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= step) AmberPrimary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "onboarding_step"
        ) { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep()
                1 -> ProfileStep(
                    weightKg = weightKg,
                    onWeightChange = { weightKg = it },
                    gender = gender,
                    onGenderChange = { gender = it }
                )
                2 -> ThresholdStep(
                    threshold = threshold,
                    onThresholdChange = { threshold = it }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (step < 2) {
                    step++
                } else {
                    val weight = weightKg.toIntOrNull() ?: 75
                    onComplete(weight, gender, threshold)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmberPrimary,
                contentColor = OnAmber
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = when (step) {
                    0 -> "Accept & Continue"
                    1 -> "Next"
                    else -> "Get Started"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (step > 0) {
            TextButton(
                onClick = { step-- },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Back", color = TextSecondary)
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "\uD83C\uDF7B",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to PacePal",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AmberPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your smart drinking companion.\nTrack your intake, know your limits,\nand pace yourself for a great time.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "⚠\uFE0F Disclaimer: PacePal provides estimates only. " +
                        "BAC calculations are approximate and should not be used to determine " +
                        "if you are fit to drive or operate machinery. Always drink responsibly " +
                        "and never drink and drive.",
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ProfileStep(
    weightKg: String,
    onWeightChange: (String) -> Unit,
    gender: Gender,
    onGenderChange: (Gender) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "About You",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AmberPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This helps us estimate your BAC accurately.",
            fontSize = 16.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(40.dp))

        // Gender selection
        Text(
            text = "Gender",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenderCard(
                label = "Male",
                icon = Icons.Default.Male,
                selected = gender == Gender.MALE,
                onClick = { onGenderChange(Gender.MALE) },
                modifier = Modifier.weight(1f)
            )
            GenderCard(
                label = "Female",
                icon = Icons.Default.Female,
                selected = gender == Gender.FEMALE,
                onClick = { onGenderChange(Gender.FEMALE) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Weight input
        Text(
            text = "Weight (kg)",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = weightKg,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                    onWeightChange(newValue)
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
    }
}

@Composable
private fun GenderCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) Modifier.border(2.dp, AmberPrimary, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) DarkSurfaceVariant else DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) AmberPrimary else TextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = if (selected) AmberPrimary else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ThresholdStep(
    threshold: Double,
    onThresholdChange: (Double) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Fun Threshold",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AmberPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The maximum BAC you consider safe\nfor continuing to drink.",
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = String.format("%.2f%%", threshold),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = AmberPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = ((threshold - 0.03) / 0.01).toFloat(),
            onValueChange = { step ->
                val newThreshold = 0.03 + (step.toInt() * 0.01)
                onThresholdChange(newThreshold)
            },
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

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "You can change this anytime in Settings.",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}
