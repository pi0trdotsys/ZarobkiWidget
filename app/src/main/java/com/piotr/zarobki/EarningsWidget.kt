package com.piotr.zarobki

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
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
    val STATUS = stringPreferencesKey("status")
    val MONTH = stringPreferencesKey("month")
    val YEAR = intPreferencesKey("year")
    val GROSS = doublePreferencesKey("gross")
    val NET = doublePreferencesKey("net")
    val LAST_SYNC = stringPreferencesKey("last_sync")
    val ERROR_MSG = stringPreferencesKey("error_msg")
    val THEME = stringPreferencesKey("theme")
    val EDIT_URL = stringPreferencesKey("edit_url")
}

class EarningsWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(110.dp, 40.dp)
        private val MEDIUM = DpSize(180.dp, 40.dp)
        private val LARGE = DpSize(180.dp, 110.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val themeName = ThemeName.fromString(prefs[WidgetKeys.THEME] ?: "TECH_DARK")
            val size = LocalSize.current

            val content: @Composable (WidgetColors) -> Unit = { colors ->
                when {
                    size.width < 180.dp -> CompactContent(prefs, colors)
                    size.height < 90.dp -> MediumContent(prefs, colors)
                    else -> FullContent(prefs, colors)
                }
            }

            if (themeName == ThemeName.MATERIAL_YOU) {
                GlanceTheme {
                    val colors = WidgetColors(
                        background = GlanceTheme.colors.widgetBackground,
                        textPrimary = GlanceTheme.colors.onSurface,
                        textMuted = GlanceTheme.colors.onSurfaceVariant,
                        accentPrimary = GlanceTheme.colors.primary,
                        accentWarn = GlanceTheme.colors.tertiary,
                    )
                    content(colors)
                }
            } else {
                content(WidgetThemes.get(themeName))
            }
        }
    }

    private fun resolveAction(prefs: Preferences): Action {
        val editUrl = prefs[WidgetKeys.EDIT_URL] ?: ""
        return if (editUrl.isNotBlank()) {
            actionStartActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(editUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } else {
            actionRunCallback<RefreshAction>()
        }
    }

    @Composable
    private fun CompactContent(prefs: Preferences, c: WidgetColors) {
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val month = prefs[WidgetKeys.MONTH] ?: ""
        val year = prefs[WidgetKeys.YEAR] ?: 0
        val gross = prefs[WidgetKeys.GROSS] ?: 0.0
        val net = prefs[WidgetKeys.NET] ?: 0.0
        val nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pl-PL"))

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .cornerRadius(16.dp).background(c.background)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clickable(resolveAction(prefs)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = if (month.length >= 3) "${month.take(3).uppercase()} $year" else "ZAROBKI"
            Text(label, style = TextStyle(color = c.accentPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(2.dp))
            when (status) {
                "error" -> Text("Blad", style = TextStyle(color = c.accentWarn, fontSize = 10.sp))
                "loading" -> Text("...", style = TextStyle(color = c.textMuted, fontSize = 10.sp))
                else -> Row(GlanceModifier.fillMaxWidth()) {
                    Text("B ${nf.format(gross)}", style = TextStyle(color = c.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(4.dp))
                    Text("N ${nf.format(net)}", style = TextStyle(color = c.accentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    @Composable
    private fun MediumContent(prefs: Preferences, c: WidgetColors) {
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val month = prefs[WidgetKeys.MONTH] ?: ""
        val year = prefs[WidgetKeys.YEAR] ?: 0
        val gross = prefs[WidgetKeys.GROSS] ?: 0.0
        val net = prefs[WidgetKeys.NET] ?: 0.0
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .cornerRadius(16.dp).background(c.background)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(resolveAction(prefs)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (month.isNotBlank()) "$month $year".uppercase() else "ZAROBKI",
                style = TextStyle(color = c.accentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(2.dp))
            when (status) {
                "error" -> {
                    val msg = prefs[WidgetKeys.ERROR_MSG] ?: "Blad odswiezania"
                    Text(msg, style = TextStyle(color = c.accentWarn, fontSize = 10.sp))
                }
                "loading" -> Text("Wczytywanie...", style = TextStyle(color = c.textMuted, fontSize = 10.sp))
                else -> Row(GlanceModifier.fillMaxWidth()) {
                    Text("B: ${nf.format(gross)} zl", style = TextStyle(color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(8.dp))
                    Text("N: ${nf.format(net)} zl", style = TextStyle(color = c.accentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    @Composable
    private fun FullContent(prefs: Preferences, c: WidgetColors) {
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val month = prefs[WidgetKeys.MONTH] ?: ""
        val year = prefs[WidgetKeys.YEAR] ?: 0
        val gross = prefs[WidgetKeys.GROSS] ?: 0.0
        val net = prefs[WidgetKeys.NET] ?: 0.0
        val lastSync = prefs[WidgetKeys.LAST_SYNC] ?: ""
        val errorMsg = prefs[WidgetKeys.ERROR_MSG] ?: ""
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .cornerRadius(16.dp).background(c.background)
                .padding(12.dp)
                .clickable(resolveAction(prefs)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(GlanceModifier.fillMaxWidth()) {
                Text(
                    if (month.isNotBlank()) "$month $year".uppercase() else "ZAROBKI",
                    style = TextStyle(color = c.accentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(GlanceModifier.height(6.dp))

            when (status) {
                "error" -> Text(
                    if (errorMsg.isNotBlank()) errorMsg else "Blad odswiezania",
                    style = TextStyle(color = c.accentWarn, fontSize = 11.sp)
                )
                "loading" -> Text("Wczytywanie...", style = TextStyle(color = c.textMuted, fontSize = 11.sp))
                else -> {
                    Text("Brutto: ${nf.format(gross)} zl", style = TextStyle(color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(2.dp))
                    Text("Netto: ${nf.format(net)} zl", style = TextStyle(color = c.accentPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            }

            Spacer(GlanceModifier.height(6.dp))
            val editUrl = prefs[WidgetKeys.EDIT_URL] ?: ""
            val footerText = when {
                errorMsg.isNotBlank() && lastSync.isNotBlank() -> "Dane z $lastSync · dotknij, by ponowic"
                editUrl.isNotBlank() && lastSync.isNotBlank() -> "$lastSync · dotknij = arkusz"
                lastSync.isNotBlank() -> "Aktualizacja: $lastSync"
                else -> "Dotknij, by odswiezyc"
            }
            val footerColor = if (errorMsg.isNotBlank()) c.accentWarn else c.textMuted
            Text(footerText, style = TextStyle(color = footerColor, fontSize = 9.sp))
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                val hadData = this[WidgetKeys.STATUS] == "ok"
                if (!hadData) this[WidgetKeys.STATUS] = "loading"
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
