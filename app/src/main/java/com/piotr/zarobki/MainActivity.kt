package com.piotr.zarobki

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.content.FileProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZarobkiApp() }
    }
}

@Composable
fun ZarobkiApp() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(ThemeName.TECH_DARK) }

    LaunchedEffect(settings) {
        url = settings.webAppUrl
        token = settings.secretToken
        selectedTheme = ThemeName.fromString(settings.theme)
    }

    ZarobkiTheme(selectedTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                "Zarobki Widget",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Motyw")
            Spacer(Modifier.height(8.dp))
            ThemePicker(selectedTheme) { theme ->
                selectedTheme = theme
                scope.launch {
                    settingsRepo.save(url, token, theme.name)
                    scheduleRefresh(context)
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Konfiguracja")
            Spacer(Modifier.height(8.dp))

            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
            )

            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("Adres Web App (Apps Script)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token, onValueChange = { token = it },
                label = { Text("Tajny token") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        settingsRepo.save(url, token, selectedTheme.name)
                        scheduleRefresh(context)
                        Toast.makeText(context, "Zapisano", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = if (selectedTheme == ThemeName.ARCTIC)
                        Color.White else MaterialTheme.colorScheme.background
                )
            ) { Text("Zapisz i odswiez") }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Eksport danych")
            Spacer(Modifier.height(8.dp))
            ExportSection(url, token)

            Spacer(Modifier.height(16.dp))
            Text(
                "Po zapisaniu dodaj widget na ekranie glownym (przytrzymaj pusty obszar ekranu → Widzety).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun ThemePicker(selected: ThemeName, onSelect: (ThemeName) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThemeName.entries.forEach { theme ->
            val (bg, accent1, accent2) = WidgetThemes.previewColors(theme)
            val isSelected = theme == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) Modifier.border(2.dp, accent1, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .background(bg, RoundedCornerShape(12.dp))
                    .clickable { onSelect(theme) }
                    .padding(10.dp)
                    .width(70.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(accent1))
                    Box(Modifier.size(14.dp).clip(CircleShape).background(accent2))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    theme.displayName,
                    color = if (bg == Color(0xFFF7F9FC)) Color(0xFF1E293B) else Color.White,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ExportSection(url: String, token: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    if (url.isBlank()) {
        Text(
            "Skonfiguruj adres URL powyzej, zeby eksportowac dane",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                scope.launch {
                    loading = true; statusMsg = ""
                    exportData(context, url, token, "csv") { msg -> statusMsg = msg }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.weight(1f),
        ) { Text("Eksport CSV") }

        OutlinedButton(
            onClick = {
                scope.launch {
                    loading = true; statusMsg = ""
                    exportData(context, url, token, "pdf") { msg -> statusMsg = msg }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.weight(1f),
        ) { Text("Eksport PDF") }
    }

    if (loading) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text("Pobieranie danych...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }

    if (statusMsg.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(statusMsg, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

private suspend fun exportData(
    context: Context, url: String, token: String, format: String,
    onStatus: (String) -> Unit
) {
    val result = EarningsRepository().fetchAll(url, token)
    when (result) {
        is FetchAllResult.Error -> onStatus("Blad: ${result.message}")
        is FetchAllResult.Success -> {
            val file = withContext(Dispatchers.IO) {
                if (format == "csv") buildCsv(context, result.data)
                else buildPdf(context, result.data)
            }
            shareFile(context, file, if (format == "csv") "text/csv" else "application/pdf")
            onStatus("Gotowe")
        }
    }
}

private fun buildCsv(context: Context, data: AllMonthsData): File {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }
    val sb = StringBuilder()
    sb.appendLine("Miesiac,Godziny,Brutto (PLN),Netto (PLN)")
    var totalH = 0.0; var totalG = 0.0; var totalN = 0.0
    data.months.forEach { m ->
        sb.appendLine("${m.month},${nf.format(m.hours)},${nf.format(m.gross)},${nf.format(m.net)}")
        totalH += m.hours; totalG += m.gross; totalN += m.net
    }
    sb.appendLine("RAZEM,${nf.format(totalH)},${nf.format(totalG)},${nf.format(totalN)}")

    val file = File(context.cacheDir, "zarobki_${data.year}.csv")
    file.writeText(sb.toString())
    return file
}

private fun buildPdf(context: Context, data: AllMonthsData): File {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 20f; isFakeBoldText = true; color = android.graphics.Color.BLACK
    }
    val headerPaint = Paint().apply {
        textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.DKGRAY
    }
    val cellPaint = Paint().apply { textSize = 11f; color = android.graphics.Color.BLACK }
    val totalPaint = Paint().apply {
        textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.BLACK
    }
    val linePaint = Paint().apply {
        color = android.graphics.Color.LTGRAY; strokeWidth = 0.5f
    }

    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    var y = 50f
    canvas.drawText("Zarobki ${data.year}", 40f, y, titlePaint)
    y += 30f

    val cols = floatArrayOf(40f, 180f, 280f, 400f, 500f)
    val headers = arrayOf("Lp.", "Miesiac", "Godziny", "Brutto (PLN)", "Netto (PLN)")
    headers.forEachIndexed { i, h -> canvas.drawText(h, cols[i], y, headerPaint) }
    y += 4f
    canvas.drawLine(40f, y, 555f, y, linePaint)
    y += 16f

    var totalH = 0.0; var totalG = 0.0; var totalN = 0.0
    data.months.forEachIndexed { i, m ->
        canvas.drawText("${i + 1}", cols[0], y, cellPaint)
        canvas.drawText(m.month, cols[1], y, cellPaint)
        canvas.drawText(nf.format(m.hours), cols[2], y, cellPaint)
        canvas.drawText(nf.format(m.gross), cols[3], y, cellPaint)
        canvas.drawText(nf.format(m.net), cols[4], y, cellPaint)
        totalH += m.hours; totalG += m.gross; totalN += m.net
        y += 18f
    }

    y += 4f
    canvas.drawLine(40f, y, 555f, y, linePaint)
    y += 16f
    canvas.drawText("RAZEM", cols[1], y, totalPaint)
    canvas.drawText(nf.format(totalH), cols[2], y, totalPaint)
    canvas.drawText(nf.format(totalG), cols[3], y, totalPaint)
    canvas.drawText(nf.format(totalN), cols[4], y, totalPaint)

    doc.finishPage(page)
    val file = File(context.cacheDir, "zarobki_${data.year}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return file
}

private fun shareFile(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Udostepnij plik").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun scheduleRefresh(context: Context) {
    val request = PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "earnings_refresh", ExistingPeriodicWorkPolicy.UPDATE, request
    )
}
