package com.piotr.zarobki

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
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
    val REFRESHING = booleanPreferencesKey("refreshing")
}

private val PL = Locale.forLanguageTag("pl-PL")

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

            if (themeName == ThemeName.MATERIAL_YOU) {
                GlanceTheme {
                    Content(
                        prefs,
                        WidgetColors(
                            background = GlanceTheme.colors.widgetBackground,
                            textPrimary = GlanceTheme.colors.onSurface,
                            textMuted = GlanceTheme.colors.onSurfaceVariant,
                            accentPrimary = GlanceTheme.colors.primary,
                            accentWarn = GlanceTheme.colors.tertiary,
                        )
                    )
                }
            } else {
                Content(prefs, WidgetThemes.get(themeName))
            }
        }
    }

    @Composable
    private fun Content(prefs: Preferences, c: WidgetColors) {
        val size = LocalSize.current
        when {
            size.width < 180.dp -> CompactContent(prefs, c)
            size.height < 90.dp -> MediumContent(prefs, c)
            else -> FullContent(prefs, c)
        }
    }

    @Composable
    private fun Shell(
        c: WidgetColors,
        hPad: Dp,
        vPad: Dp,
        buttonWidth: Dp,
        plusSize: TextUnit,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize()
                .cornerRadius(16.dp).background(c.background)
                .padding(horizontal = hPad, vertical = vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    .clickable(actionRunCallback<RefreshAction>()),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            Spacer(GlanceModifier.width(4.dp))
            Box(
                modifier = GlanceModifier.width(buttonWidth).fillMaxHeight()
                    .cornerRadius(12.dp).background(c.accentPrimary)
                    .clickable(actionStartActivity<QuickEntryActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = TextStyle(color = c.background, fontSize = plusSize, fontWeight = FontWeight.Bold))
            }
        }
    }

    private fun header(prefs: Preferences, short: Boolean): String {
        val month = prefs[WidgetKeys.MONTH] ?: ""
        val year = prefs[WidgetKeys.YEAR] ?: 0
        val base = when {
            month.isBlank() -> "ZAROBKI"
            short -> month.take(3).uppercase(PL)
            else -> "$month $year".uppercase(PL)
        }
        return if (prefs[WidgetKeys.REFRESHING] == true) "$base ↻" else base
    }

    @Composable
    private fun CompactContent(prefs: Preferences, c: WidgetColors) {
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val nf = NumberFormat.getIntegerInstance(PL)

        Shell(c, hPad = 6.dp, vPad = 3.dp, buttonWidth = 24.dp, plusSize = 16.sp) {
            Text(header(prefs, short = true), maxLines = 1,
                style = TextStyle(color = c.accentPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold))
            when (status) {
                "error" -> Text("Błąd", style = TextStyle(color = c.accentWarn, fontSize = 10.sp))
                "loading" -> Text("...", style = TextStyle(color = c.textMuted, fontSize = 10.sp))
                else -> {
                    Text("B ${nf.format(prefs[WidgetKeys.GROSS] ?: 0.0)}", maxLines = 1,
                        style = TextStyle(color = c.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    Text("N ${nf.format(prefs[WidgetKeys.NET] ?: 0.0)}", maxLines = 1,
                        style = TextStyle(color = c.accentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    @Composable
    private fun MediumContent(prefs: Preferences, c: WidgetColors) {
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val nf = NumberFormat.getIntegerInstance(PL)

        Shell(c, hPad = 10.dp, vPad = 6.dp, buttonWidth = 32.dp, plusSize = 20.sp) {
            Text(header(prefs, short = false), maxLines = 1,
                style = TextStyle(color = c.accentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(2.dp))
            when (status) {
                "error" -> Text(prefs[WidgetKeys.ERROR_MSG] ?: "Błąd odświeżania", maxLines = 2,
                    style = TextStyle(color = c.accentWarn, fontSize = 10.sp))
                "loading" -> Text("Wczytywanie...", style = TextStyle(color = c.textMuted, fontSize = 10.sp))
                else -> Row {
                    Text("B ${nf.format(prefs[WidgetKeys.GROSS] ?: 0.0)} zł", maxLines = 1,
                        style = TextStyle(color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(8.dp))
                    Text("N ${nf.format(prefs[WidgetKeys.NET] ?: 0.0)} zł", maxLines = 1,
                        style = TextStyle(color = c.accentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    @Composable
    private fun FullContent(prefs: Preferences, c: WidgetColors) {
        val status = prefs[WidgetKeys.STATUS] ?: "loading"
        val lastSync = prefs[WidgetKeys.LAST_SYNC] ?: ""
        val errorMsg = prefs[WidgetKeys.ERROR_MSG] ?: ""
        val nf = NumberFormat.getNumberInstance(PL).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }

        Shell(c, hPad = 12.dp, vPad = 12.dp, buttonWidth = 40.dp, plusSize = 24.sp) {
            Text(header(prefs, short = false), maxLines = 1,
                style = TextStyle(color = c.accentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))

            when (status) {
                "error" -> Text(errorMsg.ifBlank { "Błąd odświeżania" }, maxLines = 3,
                    style = TextStyle(color = c.accentWarn, fontSize = 11.sp))
                "loading" -> Text("Wczytywanie...", style = TextStyle(color = c.textMuted, fontSize = 11.sp))
                else -> {
                    Text("Brutto: ${nf.format(prefs[WidgetKeys.GROSS] ?: 0.0)} zł", maxLines = 1,
                        style = TextStyle(color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(2.dp))
                    Text("Netto: ${nf.format(prefs[WidgetKeys.NET] ?: 0.0)} zł", maxLines = 1,
                        style = TextStyle(color = c.accentPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            }

            Spacer(GlanceModifier.height(6.dp))
            val footer = when {
                errorMsg.isNotBlank() && lastSync.isNotBlank() -> "Dane z $lastSync · dotknij, by ponowić"
                lastSync.isNotBlank() -> "Aktualizacja: $lastSync"
                else -> "Dotknij, by odświeżyć"
            }
            Text(footer, maxLines = 1,
                style = TextStyle(color = if (errorMsg.isNotBlank()) c.accentWarn else c.textMuted, fontSize = 9.sp))
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[WidgetKeys.REFRESHING] = true
                if (this[WidgetKeys.STATUS] != "ok") this[WidgetKeys.STATUS] = "loading"
            }
        }
        EarningsWidget().update(context, glanceId)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }
}

suspend fun updateAllWidgetInstances(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(EarningsWidget::class.java).forEach { id -> EarningsWidget().update(context, id) }
}
