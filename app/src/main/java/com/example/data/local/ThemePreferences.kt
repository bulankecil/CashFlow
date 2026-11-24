package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap")
}

private val Context.fintekDataStore: DataStore<Preferences> by preferencesDataStore(name = "fintek_prefs")

class ThemePreferences(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val hasSeededOnceKey = booleanPreferencesKey("has_seeded_once")

    val themeModeFlow: Flow<ThemeMode> = context.fintekDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.fintekDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun hasSeededOnce(): Boolean {
        return context.fintekDataStore.data.first()[hasSeededOnceKey] ?: false
    }

    suspend fun markSeededOnce() {
        context.fintekDataStore.edit { prefs ->
            prefs[hasSeededOnceKey] = true
        }
    }
}
