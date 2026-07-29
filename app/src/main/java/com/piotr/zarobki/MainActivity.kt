package com.piotr.zarobki

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    secondary = Color(0xFFFBBF24),
    background = Color(0xFF0B1220),
    surface = Color(0xFF111A2E),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            MaterialTheme(colorScheme = DarkScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SettingsScreen(settingsRepo)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(settingsRepo: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val current = settingsRepo.current()
        url = current.webAppUrl
        token = current.secretToken
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("⚙ Zarobki Widget", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Podaj adres URL wdrożonej Google Apps Script (Web App) oraz sekretny token z Code.gs.",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Web App URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Secret token") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    settingsRepo.save(url, token)
                    WorkManager.getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
                    Toast.makeText(context, "Zapisano. Widget odświeży się za chwilę.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zapisz i odśwież widget")
        }

        Text(
            "Po zapisaniu dodaj widget \"Zarobki Widget\" na ekranie głównym (przytrzymaj pusty obszar ekranu → Widżety).",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}
