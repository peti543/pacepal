package com.pacepal.pacepal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepal.pacepal.data.BacTrend
import com.pacepal.pacepal.ui.theme.*

@Composable
fun BacPanel(
    currentBac: Double,
    funThreshold: Double,
    nextSafeDrinkMinutes: Int,
    fullySoberMinutes: Int,
    trend: BacTrend,
    modifier: Modifier = Modifier
) {
    val bacColor = when {
        currentBac <= funThreshold -> SafeGreen
        currentBac <= funThreshold + 0.02 -> WarningAmber
        else -> DangerRed
    }

    val trendArrow = when (trend) {
        BacTrend.RISING -> " ↑"
        BacTrend.FALLING -> " ↓"
        BacTrend.PLATEAU -> " →"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // BAC Display
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format("%.3f%%", currentBac),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = bacColor
                )
                Text(
                    text = trendArrow,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = bacColor
                )
            }
            Text(
                text = "Estimated BAC",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimerInfo(
                    label = "Next safe drink",
                    value = if (nextSafeDrinkMinutes <= 0) "You're good!"
                    else formatMinutes(nextSafeDrinkMinutes),
                    valueColor = if (nextSafeDrinkMinutes <= 0) SafeGreen else WarningAmber
                )
                TimerInfo(
                    label = "Fully sober",
                    value = if (fullySoberMinutes <= 0) "Sober"
                    else formatMinutes(fullySoberMinutes),
                    valueColor = if (fullySoberMinutes <= 0) SafeGreen else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TimerInfo(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
