package com.pacepal.pacepal.data

enum class Gender { MALE, FEMALE }

enum class DrinkCategory { BEER, SPIRIT, WINE }

data class DrinkOption(
    val name: String,
    val category: DrinkCategory,
    val volumeMl: Int,
    val abvPercent: Double,
    val emoji: String
) {
    val alcoholGrams: Double
        get() = volumeMl * (abvPercent / 100.0) * 0.789
}

data class LoggedDrink(
    val id: Long,
    val drinkName: String,
    val category: DrinkCategory,
    val volumeMl: Int,
    val abvPercent: Double,
    val alcoholGrams: Double,
    val timestampMillis: Long
)

data class UserProfile(
    val weightKg: Int = 75,
    val gender: Gender = Gender.MALE,
    val funThreshold: Double = 0.05
)

data class SessionState(
    val drinks: List<LoggedDrink> = emptyList(),
    val currentBac: Double = 0.0,
    val nextSafeDrinkMinutes: Int = 0,
    val fullySoberMinutes: Int = 0,
    val bacTrend: BacTrend = BacTrend.PLATEAU,
    val sessionComplete: Boolean = false
)

enum class BacTrend { RISING, FALLING, PLATEAU }

object DrinkOptions {
    val beers = listOf(
        DrinkOption("Pint", DrinkCategory.BEER, 568, 5.0, "\uD83C\uDF7A"),
        DrinkOption("Half Liter", DrinkCategory.BEER, 500, 5.0, "\uD83C\uDF7A"),
        DrinkOption("330ml Bottle", DrinkCategory.BEER, 330, 5.0, "\uD83C\uDF7A"),
        DrinkOption("Half Pint", DrinkCategory.BEER, 284, 5.0, "\uD83C\uDF7A"),
    )

    val spirits = listOf(
        DrinkOption("45% — 4cl", DrinkCategory.SPIRIT, 40, 45.0, "\uD83E\uDD43"),
        DrinkOption("45% — 2cl", DrinkCategory.SPIRIT, 20, 45.0, "\uD83E\uDD43"),
        DrinkOption("40% — 4cl", DrinkCategory.SPIRIT, 40, 40.0, "\uD83E\uDD43"),
        DrinkOption("40% — 2cl", DrinkCategory.SPIRIT, 20, 40.0, "\uD83E\uDD43"),
    )

    val wines = listOf(
        DrinkOption("Small Glass", DrinkCategory.WINE, 125, 12.0, "\uD83C\uDF77"),
        DrinkOption("Standard Glass", DrinkCategory.WINE, 150, 12.0, "\uD83C\uDF77"),
        DrinkOption("Medium Glass", DrinkCategory.WINE, 175, 12.0, "\uD83C\uDF77"),
        DrinkOption("Large Glass", DrinkCategory.WINE, 250, 12.0, "\uD83C\uDF77"),
    )
}
