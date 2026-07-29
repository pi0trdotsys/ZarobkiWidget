package com.piotr.zarobki

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EarningsData(
    val month: String,
    val year: Int,
    val hours: Double,
    val gross: Double,
    val net: Double
)

sealed class FetchResult {
    data class Success(val data: EarningsData) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

class EarningsRepository {

    suspend fun fetch(webAppUrl: String, secretToken: String): FetchResult =
        withContext(Dispatchers.IO) {
            if (webAppUrl.isBlank()) {
                return@withContext FetchResult.Error("Brak skonfigurowanego adresu URL")
            }
            try {
                val uri = Uri.parse(webAppUrl).buildUpon()
                    .appendQueryParameter("token", secretToken)
                    .build()
                val connection = URL(uri.toString()).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = true

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                connection.disconnect()

                if (code !in 200..299) {
                    return@withContext FetchResult.Error("Blad serwera (HTTP $code)")
                }

                val json = JSONObject(body)
                if (json.has("error")) {
                    return@withContext FetchResult.Error(json.getString("error"))
                }

                FetchResult.Success(
                    EarningsData(
                        month = json.optString("month", "-"),
                        year = json.optInt("year", 0),
                        hours = json.optDouble("hours", 0.0),
                        gross = json.optDouble("gross", 0.0),
                        net = json.optDouble("net", 0.0)
                    )
                )
            } catch (e: Exception) {
                FetchResult.Error(e.message ?: "Nieznany blad polaczenia")
            }
        }
}
