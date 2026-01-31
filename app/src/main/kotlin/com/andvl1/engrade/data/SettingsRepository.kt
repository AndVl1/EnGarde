package com.andvl1.engrade.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.andvl1.engrade.domain.model.BoutConfig
import com.andvl1.engrade.domain.model.Weapon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val WEAPON = stringPreferencesKey("weapon")
        val MODE = intPreferencesKey("mode")
        val SHOW_DOUBLE = booleanPreferencesKey("show_double")
        val ANYWHERE_TO_START = booleanPreferencesKey("anywhere_to_start")
        val BLACK_BACKGROUND = booleanPreferencesKey("black_background")
    }

    val boutConfigFlow: Flow<BoutConfig> = context.dataStore.data.map { preferences ->
        val weaponString = preferences[PreferenceKeys.WEAPON] ?: "sabre"
        val weapon = when (weaponString) {
            "sabre", "сабля", "0" -> Weapon.SABRE
            else -> Weapon.FOIL_EPEE
        }

        val mode = preferences[PreferenceKeys.MODE] ?: 5
        val showDouble = preferences[PreferenceKeys.SHOW_DOUBLE] ?: true
        val anywhereToStart = preferences[PreferenceKeys.ANYWHERE_TO_START] ?: true

        BoutConfig(
            mode = mode,
            weapon = weapon,
            periodLengthMs = 3 * 60 * 1000L,
            breakLengthMs = 1 * 60 * 1000L,
            priorityLengthMs = 1 * 60 * 1000L,
            showDoubleTouchButton = showDouble,
            anywhereToStart = anywhereToStart
        )
    }

    val useBlackBackground: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BLACK_BACKGROUND] ?: false
    }

    suspend fun updateWeapon(weapon: Weapon) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.WEAPON] = when (weapon) {
                Weapon.SABRE -> "sabre"
                Weapon.FOIL_EPEE -> "foil"
            }
        }
    }

    suspend fun updateMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MODE] = mode
        }
    }

    suspend fun updateShowDouble(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_DOUBLE] = show
        }
    }

    suspend fun updateAnywhereToStart(anywhere: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.ANYWHERE_TO_START] = anywhere
        }
    }

    suspend fun updateBlackBackground(black: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.BLACK_BACKGROUND] = black
        }
    }
}
