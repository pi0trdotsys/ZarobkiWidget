package com.piotr.zarobki

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zarobki_settings")

data class AppSettings(
    val webAppUrl: String = "",
    val secretToken: String = ""
) {
    val isConfigured: Boolean get() = webAppUrl.isNotBlank()
}

object SettingsKeys {
    val WEB_APP_URL = stringPreferencesKey("web_app_url")
    val SECRET_TOKEN = stringPreferencesKey("secret_token")
}

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            webAppUrl = prefs[SettingsKeys.WEB_APP_URL] ?: "",
            secretToken = prefs[SettingsKeys.SECRET_TOKEN] ?: ""
        )
    }

    suspend fun current(): AppSettings = settingsFlow.first()

    suspend fun save(webAppUrl: String, secretToken: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.WEB_APP_URL] = webAppUrl.trim()
            prefs[SettingsKeys.SECRET_TOKEN] = secretToken.trim()
        }
    }
}
