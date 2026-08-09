package com.privategallery.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_preferences")

enum class GridSizePreference { TWO, THREE, FOUR }
enum class SortOrderPreference { DATE_NEWEST, DATE_OLDEST, NAME_A_Z, NAME_Z_A }
enum class ThemePreference { LIGHT, DARK, SYSTEM }

/**
 * General app preferences — deliberately kept separate from
 * SecurityPreferences (STEP 45) so credential-adjacent data never
 * shares a file with ordinary UI settings like grid size or theme.
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val GRID_SIZE = stringPreferencesKey("grid_size")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val THEME = stringPreferencesKey("theme")
        val SHOW_FILE_NAMES = booleanPreferencesKey("show_file_names")
        val SHOW_VIDEO_DURATION = booleanPreferencesKey("show_video_duration")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val TRASH_RETENTION_DAYS = stringPreferencesKey("trash_retention_days")
        val REPLACE_ORIGINAL_ON_EDIT = booleanPreferencesKey("replace_original_on_edit")
        val PRESERVE_METADATA = booleanPreferencesKey("preserve_metadata")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
    }

    val isOnboardingComplete: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.appDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    val gridSize: Flow<GridSizePreference> = context.appDataStore.data.map { prefs ->
        prefs[Keys.GRID_SIZE]?.let { GridSizePreference.valueOf(it) } ?: GridSizePreference.THREE
    }

    suspend fun setGridSize(value: GridSizePreference) {
        context.appDataStore.edit { it[Keys.GRID_SIZE] = value.name }
    }

    val sortOrder: Flow<SortOrderPreference> = context.appDataStore.data.map { prefs ->
        prefs[Keys.SORT_ORDER]?.let { SortOrderPreference.valueOf(it) } ?: SortOrderPreference.DATE_NEWEST
    }

    suspend fun setSortOrder(value: SortOrderPreference) {
        context.appDataStore.edit { it[Keys.SORT_ORDER] = value.name }
    }

    val theme: Flow<ThemePreference> = context.appDataStore.data.map { prefs ->
        prefs[Keys.THEME]?.let { ThemePreference.valueOf(it) } ?: ThemePreference.SYSTEM
    }

    suspend fun setTheme(value: ThemePreference) {
        context.appDataStore.edit { it[Keys.THEME] = value.name }
    }

    val showFileNames: Flow<Boolean> = context.appDataStore.data.map { it[Keys.SHOW_FILE_NAMES] ?: false }
    suspend fun setShowFileNames(value: Boolean) {
        context.appDataStore.edit { it[Keys.SHOW_FILE_NAMES] = value }
    }

    val showVideoDuration: Flow<Boolean> = context.appDataStore.data.map { it[Keys.SHOW_VIDEO_DURATION] ?: true }
    suspend fun setShowVideoDuration(value: Boolean) {
        context.appDataStore.edit { it[Keys.SHOW_VIDEO_DURATION] = value }
    }

    val animationsEnabled: Flow<Boolean> = context.appDataStore.data.map { it[Keys.ANIMATIONS_ENABLED] ?: true }
    suspend fun setAnimationsEnabled(value: Boolean) {
        context.appDataStore.edit { it[Keys.ANIMATIONS_ENABLED] = value }
    }

    val trashRetentionDays: Flow<Int> = context.appDataStore.data.map { prefs ->
        prefs[Keys.TRASH_RETENTION_DAYS]?.toIntOrNull() ?: 30
    }
    suspend fun setTrashRetentionDays(days: Int) {
        context.appDataStore.edit { it[Keys.TRASH_RETENTION_DAYS] = days.toString() }
    }

    val replaceOriginalOnEdit: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.REPLACE_ORIGINAL_ON_EDIT] ?: false }
    suspend fun setReplaceOriginalOnEdit(value: Boolean) {
        context.appDataStore.edit { it[Keys.REPLACE_ORIGINAL_ON_EDIT] = value }
    }

    val preserveMetadata: Flow<Boolean> = context.appDataStore.data.map { it[Keys.PRESERVE_METADATA] ?: true }
    suspend fun setPreserveMetadata(value: Boolean) {
        context.appDataStore.edit { it[Keys.PRESERVE_METADATA] = value }
    }

    val screenshotProtectionEnabled: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.SCREENSHOT_PROTECTION] ?: true }
    suspend fun setScreenshotProtectionEnabled(value: Boolean) {
        context.appDataStore.edit { it[Keys.SCREENSHOT_PROTECTION] = value }
    }
}
