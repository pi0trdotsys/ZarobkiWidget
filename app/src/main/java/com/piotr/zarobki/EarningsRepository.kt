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
    val net: Double,
    val editUrl: String = ""
)

data class MonthData(
    val month: String,
    val hours: Double,
    val gross: Double,
    val net: Double
)

data class AllMonthsData(
    val year: Int,
    val months: List<MonthData>
)

sealed class FetchResult {
    data class Success(val data: EarningsData) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

sealed class FetchAllResult {
    data class Success(val data: AllMonthsData) : FetchAllResult()
    data class Error(val message: String) : FetchAllResult()
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
                val body = httpGet(uri.toString())
                    ?: return@withContext FetchResult.Error("Brak odpowiedzi serwera")

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
                        net = json.optDouble("net", 0.0),
                        editUrl = json.optString("editUrl", "")
                    )
                )
            } catch (e: Exception) {
                FetchResult.Error(e.message ?: "Nieznany blad polaczenia")
            }
        }

    suspend fun fetchAll(webAppUrl: String, secretToken: String): FetchAllResult =
        withContext(Dispatchers.IO) {
            if (webAppUrl.isBlank()) {
                return@withContext FetchAllResult.Error("Brak skonfigurowanego adresu URL")
            }
            try {
                val uri = Uri.parse(webAppUrl).buildUpon()
                    .appendQueryParameter("token", secretToken)
                    .appendQueryParameter("mode", "all")
                    .build()
                val body = httpGet(uri.toString())
                    ?: return@withContext FetchAllResult.Error("Brak odpowiedzi serwera")

                val json = JSONObject(body)
                if (json.has("error")) {
                    return@withContext FetchAllResult.Error(json.getString("error"))
                }

                val year = json.optInt("year", 0)
                val monthsArray = json.getJSONArray("months")
                val months = (0 until monthsArray.length()).map { i ->
                    val m = monthsArray.getJSONObject(i)
                    MonthData(
                        month = m.optString("month", "-"),
                        hours = m.optDouble("hours", 0.0),
                        gross = m.optDouble("gross", 0.0),
                        net = m.optDouble("net", 0.0)
                    )
                }

                FetchAllResult.Success(AllMonthsData(year = year, months = months))
            } catch (e: Exception) {
                FetchAllResult.Error(e.message ?: "Nieznany blad polaczenia")
            }
        }

    private fun httpGet(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }
        connection.disconnect()

        if (code !in 200..299) return null
        return body
    }
}
