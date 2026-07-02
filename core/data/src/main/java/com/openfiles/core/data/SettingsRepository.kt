package com.openfiles.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemePref { Light, Dark, TrueBlack, System }

/** Small DataStore-backed settings store: theme, grid vs list default, hidden-files toggle. */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme")
    private val showHiddenKey = booleanPreferencesKey("show_hidden")
    private val gridViewKey = booleanPreferencesKey("grid_view")

    val theme = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemePref.valueOf(it) }.getOrNull() } ?: ThemePref.System
    }

    val showHiddenFiles = context.dataStore.data.map { it[showHiddenKey] ?: false }
    val gridView = context.dataStore.data.map { it[gridViewKey] ?: false }

    suspend fun setTheme(theme: ThemePref) {
        context.dataStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { it[showHiddenKey] = show }
    }

    suspend fun setGridView(grid: Boolean) {
        context.dataStore.edit { it[gridViewKey] = grid }
    }
}
