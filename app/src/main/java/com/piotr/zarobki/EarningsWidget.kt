package com.piotr.zarobki

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.NumberFormat
import java.util.Locale

object WidgetKeys {
    val STATUS = stringPreferencesKey("status")       // "loading" | "ok" | "error"
    val MONTH = stringPreferencesKey("month")
    val YEAR = intPreferencesKey("year")
    val GROSS = doublePreferencesKey("gross")
    val NET = doublePreferencesKey("net")
    val LAST_SYNC = stringPreferencesKey("last_sync")
    val ERROR_MSG = stringPreferencesKey("error_msg")
}

private val BgDark = Color(0xFF0B1220)
private val TextLight = Color(0xFFE5E7EB)
private val TextMuted = Color(0xFF94A3B8)
private val AccentCyan = Color(0xFF22D3EE)
private val AccentAmber = Color(0xFFFBBF24)

class EarningsWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val month = prefs[WidgetKeys.MONTH] ?: ""
        val year = prefs[WidgetKeys.YEAR] ?: 0
        val gross = prefs[WidgetKeys.GROSS] ?: 0.0
        val net = prefs[WidgetKeys.NET] ?: 0.0
        val lastSync = prefs[WidgetKeys.LAST_SYNC] ?: ""
        val errorMsg = prefs[WidgetKeys.ERROR_MSG] ?: ""

        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BgDark)
                .padding(12.dp)
                .clickable(actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = if (month.isNotBlank()) "$month $year".uppercase() else "ZAROBKI",
                    style = TextStyle(color = ColorProvider(AccentCyan), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))

            when (status) {
                "error" -> {
                    Text(
                        text = if (errorMsg.isNotBlank()) errorMsg else "Blad odswiezania",
                        style = TextStyle(color = ColorProvider(AccentAmber), fontSize = 11.sp)
                    )
                }
                "loading" -> {
                    Text(
                        text = "Wczytywanie...",
                        style = TextStyle(color = ColorProvider(TextMuted), fontSize = 11.sp)
                    )
                }
                else -> {
                    Text(
                        text = "Brutto: ${nf.format(gross)} zl",
                        style = TextStyle(color = ColorProvider(TextLight), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Netto: ${nf.format(net)} zl",
                        style = TextStyle(color = ColorProvider(AccentCyan), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = if (lastSync.isNotBlank()) "Aktualizacja: $lastSync (dotknij, by odswiezyc)" else "Dotknij, by odswiezyc",
                style = TextStyle(color = ColorProvider(TextMuted), fontSize = 9.sp)
            )
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[WidgetKeys.STATUS] = "loading"
            }
        }
        EarningsWidget().update(context, glanceId)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }
}

suspend fun updateAllWidgetInstances(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val ids = manager.getGlanceIds(EarningsWidget::class.java)
    ids.forEach { id -> EarningsWidget().update(context, id) }
}
