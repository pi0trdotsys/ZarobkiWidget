package com.piotr.zarobki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class QuickEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuickEntryScreen(onClose = { finish() }) }
    }
}

private val PL = Locale.forLanguageTag("pl-PL")
private val QUICK_HOURS = listOf(4.0, 6.0, 8.0, 10.0, 12.0)

private fun formatHours(h: Double): String =
    if (h % 1.0 == 0.0) h.toInt().toString() else String.format(PL, "%.1f", h)

@Composable
private fun QuickEntryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf<AppSettings?>(null) }
    LaunchedEffect(Unit) { settings = SettingsRepository(context).current() }

    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        val s = settings ?: return@Box
        ZarobkiTheme(ThemeName.fromString(s.theme)) {
            if (!s.isConfigured) {
                Sheet { Text("Najpierw skonfiguruj adres URL w aplikacji.", color = MaterialTheme.colorScheme.onSurface) }
            } else {
                QuickEntrySheet(s, onClose)
            }
        }
    }
}

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = noRipple, indication = null) {}
            .navigationBarsPadding()
            .padding(20.dp)
    ) { content() }
}

@Composable
private fun QuickEntrySheet(settings: AppSettings, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { EarningsRepository() }
    val colors = MaterialTheme.colorScheme

    var dayOffset by remember { mutableIntStateOf(0) }
    var hours by remember { mutableDoubleStateOf(8.0) }
    var sheetHours by remember { mutableStateOf<Double?>(null) }
    var editUrl by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("Wczytywanie...") }
    var saving by remember { mutableStateOf(false) }

    val date = LocalDate.now().plusDays(dayOffset.toLong())
    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", PL))

    LaunchedEffect(dayOffset) {
        sheetHours = null
        editUrl = ""
        info = "Wczytywanie..."
        when (val r = repo.fetchDay(settings.webAppUrl, settings.secretToken, date.toString())) {
            is DayResult.Success -> {
                sheetHours = r.data.hours
                editUrl = r.data.editUrl
                if (r.data.hours > 0) hours = r.data.hours
                info = if (r.data.hours > 0) "W arkuszu: ${formatHours(r.data.hours)} h" else "Brak wpisu"
            }
            is DayResult.Error -> info = r.message
        }
    }

    Sheet {
        Text("Wpisz godziny", color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(dateLabel, color = colors.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Dziś", -1 to "Wczoraj").forEach { (offset, label) ->
                Chip(label, selected = dayOffset == offset, modifier = Modifier.weight(1f)) {
                    dayOffset = offset
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton("−") { hours = (hours - 0.5).coerceAtLeast(0.0) }
            Text(
                "${formatHours(hours)} h",
                color = colors.primary, fontSize = 44.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.width(150.dp)
            )
            StepButton("+") { hours = (hours + 0.5).coerceAtMost(24.0) }
        }
        Text(
            info, color = colors.onSurfaceVariant, fontSize = 12.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QUICK_HOURS.forEach { h ->
                Chip("${formatHours(h)} h", selected = hours == h, modifier = Modifier.weight(1f)) { hours = h }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(editUrl)))
                    onClose()
                },
                enabled = editUrl.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Otwórz arkusz") }

            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        when (val r = repo.setHours(settings.webAppUrl, settings.secretToken, date.toString(), hours)) {
                            is FetchResult.Success -> {
                                storeEarnings(context, r.data, settings.theme)
                                val msg = if (hours > 0) "Zapisano ${formatHours(hours)} h" else "Usunięto wpis"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                            is FetchResult.Error -> {
                                info = r.message
                                saving = false
                            }
                        }
                    }
                },
                enabled = !saving && sheetHours != hours,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.background)
                } else {
                    Text(if (hours > 0) "Zapisz" else "Usuń wpis")
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primary else Color.Transparent)
            .border(1.dp, if (selected) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.background else colors.onSurface
        )
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .border(1.5.dp, colors.primary, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.primary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}
