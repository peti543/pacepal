package com.pacepal.pacepal

import com.pacepal.pacepal.data.*
import org.junit.Assert.*
import org.junit.Test

class BacCalculatorTest {

    private fun makeDrink(
        alcoholGrams: Double,
        timestampMillis: Long,
        id: Long = timestampMillis
    ) = LoggedDrink(
        id = id,
        drinkName = "Test",
        category = DrinkCategory.BEER,
        volumeMl = 500,
        abvPercent = 5.0,
        alcoholGrams = alcoholGrams,
        timestampMillis = timestampMillis
    )

    @Test
    fun `empty drinks returns zero BAC`() {
        val bac = BacCalculator.calculateCurrentBac(
            drinks = emptyList(),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = System.currentTimeMillis()
        )
        assertEquals(0.0, bac, 0.0001)
    }

    @Test
    fun `single pint for 75kg male at time of drinking`() {
        // Pint of 5% beer: 568ml * 0.05 * 0.789 = 22.41g alcohol
        val now = System.currentTimeMillis()
        val alcoholGrams = 568 * 0.05 * 0.789 // ~22.41g
        val drink = makeDrink(alcoholGrams, now)

        val bac = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = now
        )

        // BAC = 22.41 / (0.68 * 75 * 10) = 22.41 / 510 = ~0.0439
        assertTrue("BAC should be around 0.044, got $bac", bac in 0.04..0.05)
    }

    @Test
    fun `single pint for 60kg female gives higher BAC`() {
        val now = System.currentTimeMillis()
        val alcoholGrams = 568 * 0.05 * 0.789
        val drink = makeDrink(alcoholGrams, now)

        val bacMale = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = now
        )

        val bacFemale = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink),
            weightKg = 60,
            gender = Gender.FEMALE,
            currentTimeMillis = now
        )

        assertTrue("Female BAC should be higher than male", bacFemale > bacMale)
    }

    @Test
    fun `BAC decreases over time`() {
        val drinkTime = 1_000_000_000L
        val alcoholGrams = 568 * 0.05 * 0.789
        val drink = makeDrink(alcoholGrams, drinkTime)

        val bacAtDrinkTime = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = drinkTime
        )

        val oneHourLater = drinkTime + 3_600_000
        val bacOneHourLater = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = oneHourLater
        )

        assertTrue("BAC should decrease after 1 hour", bacOneHourLater < bacAtDrinkTime)
        // Should decrease by approximately 0.015 (elimination rate)
        val decrease = bacAtDrinkTime - bacOneHourLater
        assertEquals(0.015, decrease, 0.002)
    }

    @Test
    fun `BAC never goes below zero`() {
        val drinkTime = 1_000_000_000L
        val alcoholGrams = 5.0 // small amount
        val drink = makeDrink(alcoholGrams, drinkTime)

        // 24 hours later - should be fully metabolized
        val muchLater = drinkTime + 86_400_000
        val bac = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = muchLater
        )

        assertEquals(0.0, bac, 0.0001)
    }

    @Test
    fun `multiple drinks stack up`() {
        val now = System.currentTimeMillis()
        val alcoholGrams = 568 * 0.05 * 0.789
        val drink1 = makeDrink(alcoholGrams, now, id = 1)
        val drink2 = makeDrink(alcoholGrams, now, id = 2)

        val bacOne = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink1),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = now
        )

        val bacTwo = BacCalculator.calculateCurrentBac(
            drinks = listOf(drink1, drink2),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = now
        )

        assertEquals(bacOne * 2, bacTwo, 0.001)
    }

    @Test
    fun `minutes until sober returns zero when already sober`() {
        val minutes = BacCalculator.minutesUntilBacReaches(
            targetBac = 0.0,
            drinks = emptyList(),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = System.currentTimeMillis()
        )
        assertEquals(0, minutes)
    }

    @Test
    fun `minutes until threshold returns positive when above threshold`() {
        val now = System.currentTimeMillis()
        val alcoholGrams = 568 * 0.05 * 0.789
        val drink = makeDrink(alcoholGrams, now)

        val minutes = BacCalculator.minutesUntilBacReaches(
            targetBac = 0.0,
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = now
        )

        assertTrue("Should take time to reach sober, got $minutes", minutes > 0)
    }

    @Test
    fun `trend is plateau with no drinks`() {
        val trend = BacCalculator.determineTrend(
            drinks = emptyList(),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = System.currentTimeMillis()
        )
        assertEquals(BacTrend.PLATEAU, trend)
    }

    @Test
    fun `trend is falling after some time`() {
        val drinkTime = System.currentTimeMillis() - 3_600_000 // 1 hour ago
        val alcoholGrams = 568 * 0.05 * 0.789
        val drink = makeDrink(alcoholGrams, drinkTime)

        val trend = BacCalculator.determineTrend(
            drinks = listOf(drink),
            weightKg = 75,
            gender = Gender.MALE,
            currentTimeMillis = System.currentTimeMillis()
        )
        assertEquals(BacTrend.FALLING, trend)
    }

    @Test
    fun `drink alcohol grams calculation`() {
        // Pint: 568ml, 5% ABV
        val pint = DrinkOption("Pint", DrinkCategory.BEER, 568, 5.0, "🍺")
        val expected = 568 * 0.05 * 0.789 // = 22.4076
        assertEquals(expected, pint.alcoholGrams, 0.001)
    }
}
