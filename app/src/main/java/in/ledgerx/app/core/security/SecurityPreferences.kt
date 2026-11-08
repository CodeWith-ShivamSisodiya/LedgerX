package com.ledgerx.app.core.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ledgerXPreferences by preferencesDataStore(name = "ledgerx_settings")

@Singleton
class SecurityPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val appLockEnabled: Flow<Boolean> = context.ledgerXPreferences.data.map { values ->
        values[APP_LOCK_ENABLED] ?: false
    }
    val themeMode: Flow<String> = context.ledgerXPreferences.data.map { it[THEME_MODE] ?: "SYSTEM" }
    val lastAccountId: Flow<String?> = context.ledgerXPreferences.data.map { it[LAST_ACCOUNT_ID] }
    val lastCategoryId: Flow<String?> = context.ledgerXPreferences.data.map { it[LAST_CATEGORY_ID] }
    val textScale: Flow<Float> = context.ledgerXPreferences.data.map { it[TEXT_SCALE] ?: 1f }
    val remindersEnabled: Flow<Boolean> = context.ledgerXPreferences.data.map { it[REMINDERS_ENABLED] ?: true }
    val localeTag: Flow<String> = context.ledgerXPreferences.data.map { it[LOCALE_TAG] ?: "en-IN" }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.ledgerXPreferences.edit { values -> values[APP_LOCK_ENABLED] = enabled }
    }
    suspend fun setThemeMode(mode: String) {
        require(mode in setOf("SYSTEM", "LIGHT", "DARK"))
        context.ledgerXPreferences.edit { it[THEME_MODE] = mode }
    }
    suspend fun setLastAccountId(accountId: String) { context.ledgerXPreferences.edit { it[LAST_ACCOUNT_ID] = accountId } }
    suspend fun setLastCategoryId(categoryId: String) { context.ledgerXPreferences.edit { it[LAST_CATEGORY_ID] = categoryId } }
    suspend fun setTextScale(scale: Float) { require(scale in 1f..1.3f); context.ledgerXPreferences.edit { it[TEXT_SCALE] = scale } }
    suspend fun setRemindersEnabled(enabled: Boolean) { context.ledgerXPreferences.edit { it[REMINDERS_ENABLED] = enabled } }
    suspend fun setLocaleTag(tag: String) { require(tag in setOf("en-IN","hi-IN")); context.ledgerXPreferences.edit { it[LOCALE_TAG] = tag } }
    suspend fun clear() { context.ledgerXPreferences.edit { it.clear() } }

    private companion object {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_ACCOUNT_ID = stringPreferencesKey("last_account_id")
        val LAST_CATEGORY_ID = stringPreferencesKey("last_category_id")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val LOCALE_TAG = stringPreferencesKey("locale_tag")
    }
}
