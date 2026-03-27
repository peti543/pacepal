package com.pacepal.pacepal.data

import kotlin.math.max

object BacCalculator {

    private const val ELIMINATION_RATE = 0.015 // % per hour
    private const val MALE_WIDMARK = 0.68
    private const val FEMALE_WIDMARK = 0.55

    fun calculateCurrentBac(
        drinks: List<LoggedDrink>,
        weightKg: Int,
        gender: Gender,
        currentTimeMillis: Long
    ): Double {
        if (drinks.isEmpty()) return 0.0

        val widmarkFactor = when (gender) {
            Gender.MALE -> MALE_WIDMARK
            Gender.FEMALE -> FEMALE_WIDMARK
        }

        var totalBac = 0.0
        for (drink in drinks) {
            val hoursElapsed = (currentTimeMillis - drink.timestampMillis) / 3_600_000.0
            val drinkBac = (drink.alcoholGrams / (widmarkFactor * weightKg * 10.0))
            val bacAfterElimination = drinkBac - (ELIMINATION_RATE * hoursElapsed)
            totalBac += max(0.0, bacAfterElimination)
        }

        return max(0.0, totalBac)
    }

    fun minutesUntilBacReaches(
        targetBac: Double,
        drinks: List<LoggedDrink>,
        weightKg: Int,
        gender: Gender,
        currentTimeMillis: Long
    ): Int {
        val currentBac = calculateCurrentBac(drinks, weightKg, gender, currentTimeMillis)
        if (currentBac <= targetBac) return 0

        val diff = currentBac - targetBac
        val hoursNeeded = diff / ELIMINATION_RATE
        return (hoursNeeded * 60).toInt() + 1
    }

    fun determineTrend(
        drinks: List<LoggedDrink>,
        weightKg: Int,
        gender: Gender,
        currentTimeMillis: Long
    ): BacTrend {
        if (drinks.isEmpty()) return BacTrend.PLATEAU

        val now = calculateCurrentBac(drinks, weightKg, gender, currentTimeMillis)
        val oneMinAgo = calculateCurrentBac(drinks, weightKg, gender, currentTimeMillis - 60_000)

        return when {
            now > oneMinAgo + 0.0001 -> BacTrend.RISING
            now < oneMinAgo - 0.0001 -> BacTrend.FALLING
            else -> BacTrend.PLATEAU
        }
    }
}
