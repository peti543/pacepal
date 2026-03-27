package com.pacepal.pacepal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pacepal.pacepal.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PacePalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application)
    private val gson = Gson()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    val isOnboardingComplete: StateFlow<Boolean> = repository.isOnboardingComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _drinks = MutableStateFlow<List<LoggedDrink>>(emptyList())
    val drinks: StateFlow<List<LoggedDrink>> = _drinks.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _showAllClear = MutableStateFlow(false)
    val showAllClear: StateFlow<Boolean> = _showAllClear.asStateFlow()

    private var lastUndoDrinkId: Long? = null

    init {
        // Load persisted session drinks
        viewModelScope.launch {
            repository.sessionDrinksJson.collect { json ->
                val type = object : TypeToken<List<LoggedDrink>>() {}.type
                val loaded: List<LoggedDrink> = try {
                    gson.fromJson(json, type) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                _drinks.value = loaded
            }
        }

        // Periodic BAC update every 30 seconds
        viewModelScope.launch {
            while (true) {
                recalculateBac()
                delay(30_000)
            }
        }
    }

    fun completeOnboarding(weightKg: Int, gender: Gender, threshold: Double) {
        viewModelScope.launch {
            repository.saveProfile(UserProfile(weightKg, gender, threshold))
            repository.setOnboardingComplete()
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            recalculateBac()
        }
    }

    fun logDrink(option: DrinkOption) {
        logDrinkInternal(option.name, option.category, option.volumeMl, option.abvPercent)
    }

    fun logCustomDrink(baseDrink: DrinkOption, volumeMl: Int, abvPercent: Double) {
        val name = "${baseDrink.name} (custom)"
        logDrinkInternal(name, baseDrink.category, volumeMl, abvPercent)
    }

    private fun logDrinkInternal(name: String, category: DrinkCategory, volumeMl: Int, abvPercent: Double) {
        val alcoholGrams = volumeMl * (abvPercent / 100.0) * 0.789
        val drink = LoggedDrink(
            id = System.currentTimeMillis(),
            drinkName = name,
            category = category,
            volumeMl = volumeMl,
            abvPercent = abvPercent,
            alcoholGrams = alcoholGrams,
            timestampMillis = System.currentTimeMillis()
        )
        val updated = _drinks.value + drink
        _drinks.value = updated
        lastUndoDrinkId = drink.id
        persistDrinks(updated)
        recalculateBac()
        _snackbarMessage.value = "Logged $name"
    }

    fun undoLastDrink() {
        val undoId = lastUndoDrinkId ?: return
        val updated = _drinks.value.filter { it.id != undoId }
        _drinks.value = updated
        lastUndoDrinkId = null
        persistDrinks(updated)
        recalculateBac()
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearSession() {
        _drinks.value = emptyList()
        _sessionState.value = SessionState()
        lastUndoDrinkId = null
        viewModelScope.launch {
            repository.clearSession()
        }
    }

    fun dismissAllClear() {
        _showAllClear.value = false
    }

    private fun recalculateBac() {
        val profile = userProfile.value
        val drinkList = _drinks.value
        val now = System.currentTimeMillis()

        val currentBac = BacCalculator.calculateCurrentBac(drinkList, profile.weightKg, profile.gender, now)
        val nextSafe = BacCalculator.minutesUntilBacReaches(profile.funThreshold, drinkList, profile.weightKg, profile.gender, now)
        val fullySober = BacCalculator.minutesUntilBacReaches(0.0, drinkList, profile.weightKg, profile.gender, now)
        val trend = BacCalculator.determineTrend(drinkList, profile.weightKg, profile.gender, now)

        val prevBac = _sessionState.value.currentBac
        val wasAboveZero = prevBac > 0.001
        val nowAtZero = currentBac < 0.001 && drinkList.isNotEmpty()

        _sessionState.value = SessionState(
            drinks = drinkList,
            currentBac = currentBac,
            nextSafeDrinkMinutes = nextSafe,
            fullySoberMinutes = fullySober,
            bacTrend = trend,
            sessionComplete = nowAtZero
        )

        // "All clear" trigger
        if (wasAboveZero && nowAtZero) {
            _showAllClear.value = true
        }
    }

    private fun persistDrinks(drinks: List<LoggedDrink>) {
        viewModelScope.launch {
            repository.saveSessionDrinks(gson.toJson(drinks))
        }
    }
}
