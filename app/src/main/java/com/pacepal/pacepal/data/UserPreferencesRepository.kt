package com.pacepal.pacepal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_WEIGHT = intPreferencesKey("weight_kg")
        private val KEY_GENDER = stringPreferencesKey("gender")
        private val KEY_THRESHOLD = doublePreferencesKey("fun_threshold")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_SESSION_DRINKS = stringPreferencesKey("session_drinks")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            weightKg = prefs[KEY_WEIGHT] ?: 75,
            gender = Gender.valueOf(prefs[KEY_GENDER] ?: Gender.MALE.name),
            funThreshold = prefs[KEY_THRESHOLD] ?: 0.05
        )
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    val sessionDrinksJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SESSION_DRINKS] ?: "[]"
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEIGHT] = profile.weightKg
            prefs[KEY_GENDER] = profile.gender.name
            prefs[KEY_THRESHOLD] = profile.funThreshold
        }
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun saveSessionDrinks(json: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_DRINKS] = json
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_DRINKS] = "[]"
        }
    }
}
