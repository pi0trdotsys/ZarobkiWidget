package com.piotr.zarobki

import android.content.Context
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
        val settingsRepo = SettingsRepository(applicationContext)
        val settings = settingsRepo.current()
        val manager = GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(EarningsWidget::class.java)
        if (ids.isEmpty()) return Result.success()

        if (!settings.isConfigured) {
            ids.forEach { id ->
                updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WidgetKeys.STATUS] = "error"
                        this[WidgetKeys.ERROR_MSG] = "Otworz aplikacje i podaj adres URL"
                    }
                }
            }
            updateAllWidgetInstances(applicationContext)
            return Result.success()
        }

        val result = EarningsRepository().fetch(settings.webAppUrl, settings.secretToken)
        val timeLabel = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pl-PL")).format(Date())

        ids.forEach { id ->
            updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    when (result) {
                        is FetchResult.Success -> {
                            this[WidgetKeys.STATUS] = "ok"
                            this[WidgetKeys.MONTH] = result.data.month
                            this[WidgetKeys.YEAR] = result.data.year
                            this[WidgetKeys.GROSS] = result.data.gross
                            this[WidgetKeys.NET] = result.data.net
                            this[WidgetKeys.LAST_SYNC] = timeLabel
                            remove(WidgetKeys.ERROR_MSG)
                        }
                        is FetchResult.Error -> {
                            this[WidgetKeys.STATUS] = "error"
                            this[WidgetKeys.ERROR_MSG] = result.message
                        }
                    }
                }
            }
        }
        updateAllWidgetInstances(applicationContext)

        return if (result is FetchResult.Error) Result.retry() else Result.success()
    }
}
