package com.pacepal.pacepal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepal.pacepal.data.DrinkOption
import com.pacepal.pacepal.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrinkCard(
    drink: DrinkOption,
    isAboveThreshold: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var flashRed by remember { mutableStateOf(false) }

    val cardColor by animateColorAsState(
        targetValue = if (flashRed) DangerRed.copy(alpha = 0.3f) else DarkCard,
        animationSpec = tween(150),
        label = "card_flash"
    )

    LaunchedEffect(flashRed) {
        if (flashRed) {
            delay(300)
            flashRed = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isAboveThreshold) flashRed = true
                    onTap()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = drink.emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drink.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${drink.volumeMl}ml • ${drink.abvPercent.toInt()}%",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = String.format("%.1fg", drink.alcoholGrams),
                fontSize = 13.sp,
                color = AmberPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
