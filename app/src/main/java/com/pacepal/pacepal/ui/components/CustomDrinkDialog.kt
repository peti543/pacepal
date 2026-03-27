package com.pacepal.pacepal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacepal.pacepal.data.DrinkOption
import com.pacepal.pacepal.ui.theme.*

@Composable
fun CustomDrinkDialog(
    baseDrink: DrinkOption,
    onConfirm: (volumeMl: Int, abvPercent: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var volume by remember { mutableStateOf(baseDrink.volumeMl.toString()) }
    var abv by remember { mutableStateOf(baseDrink.abvPercent.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Customize ${baseDrink.name}",
                color = AmberPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = volume,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) volume = it },
                    label = { Text("Volume (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = AmberPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = abv,
                    onValueChange = { newVal ->
                        if (newVal.matches(Regex("^\\d{0,2}\\.?\\d{0,1}$"))) abv = newVal
                    },
                    label = { Text("ABV (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = AmberPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val volInt = volume.toIntOrNull() ?: 0
                val abvDbl = abv.toDoubleOrNull() ?: 0.0
                val grams = volInt * (abvDbl / 100.0) * 0.789
                Text(
                    text = String.format("≈ %.1fg alcohol", grams),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = volume.toIntOrNull() ?: baseDrink.volumeMl
                    val a = abv.toDoubleOrNull() ?: baseDrink.abvPercent
                    onConfirm(v, a)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = OnAmber
                )
            ) {
                Text("Log Drink", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
