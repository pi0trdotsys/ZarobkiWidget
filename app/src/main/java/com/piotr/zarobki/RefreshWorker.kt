package com.piotr.zarobki

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RefreshWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsRepository(context).current()
        val ids = GlanceAppWidgetManager(context).getGlanceIds(EarningsWidget::class.java)
        if (ids.isEmpty()) return Result.success()

        if (!settings.isConfigured) {
            updateWidgets(context) {
                this[WidgetKeys.STATUS] = "error"
                this[WidgetKeys.ERROR_MSG] = "Otwórz aplikację i podaj adres URL"
                this[WidgetKeys.REFRESHING] = false
            }
            return Result.success()
        }

        return when (val result = EarningsRepository().fetch(settings.webAppUrl, settings.secretToken)) {
            is FetchResult.Success -> {
                storeEarnings(context, result.data, settings.theme)
                Result.success()
            }
            is FetchResult.Error -> {
                updateWidgets(context) {
                    this[WidgetKeys.THEME] = settings.theme
                    if (this[WidgetKeys.STATUS] != "ok") this[WidgetKeys.STATUS] = "error"
                    this[WidgetKeys.ERROR_MSG] = result.message
                    this[WidgetKeys.REFRESHING] = false
                }
                Result.retry()
            }
        }
    }
}

suspend fun storeEarnings(context: Context, data: EarningsData, theme: String) {
    val timeLabel = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pl-PL")).format(Date())
    updateWidgets(context) {
        this[WidgetKeys.THEME] = theme
        this[WidgetKeys.STATUS] = "ok"
        this[WidgetKeys.MONTH] = data.month
        this[WidgetKeys.YEAR] = data.year
        this[WidgetKeys.GROSS] = data.gross
        this[WidgetKeys.NET] = data.net
        this[WidgetKeys.LAST_SYNC] = timeLabel
        this[WidgetKeys.EDIT_URL] = data.editUrl
        this[WidgetKeys.REFRESHING] = false
        remove(WidgetKeys.ERROR_MSG)
    }
}

suspend fun updateWidgets(context: Context, block: MutablePreferences.() -> Unit) {
    val ids = GlanceAppWidgetManager(context).getGlanceIds(EarningsWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply(block)
        }
    }
    updateAllWidgetInstances(context)
}
