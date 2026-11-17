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

data class DayInfo(
    val hours: Double,
    val editUrl: String
)

sealed class FetchResult {
    data class Success(val data: EarningsData) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

sealed class FetchAllResult {
    data class Success(val data: AllMonthsData) : FetchAllResult()
    data class Error(val message: String) : FetchAllResult()
}

sealed class DayResult {
    data class Success(val data: DayInfo) : DayResult()
    data class Error(val message: String) : DayResult()
}

private const val OUTDATED_SCRIPT = "Zaktualizuj wdrożenie Apps Script (brak obsługi wpisywania godzin)"

class EarningsRepository {

    suspend fun fetch(webAppUrl: String, secretToken: String): FetchResult =
        withContext(Dispatchers.IO) {
            try {
                val json = request(webAppUrl, secretToken)
                FetchResult.Success(parseEarnings(json))
            } catch (e: Exception) {
                FetchResult.Error(e.message ?: "Nieznany błąd połączenia")
            }
        }

    suspend fun fetchAll(webAppUrl: String, secretToken: String): FetchAllResult =
        withContext(Dispatchers.IO) {
            try {
                val json = request(webAppUrl, secretToken, "mode" to "all")
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
                FetchAllResult.Success(AllMonthsData(json.optInt("year", 0), months))
            } catch (e: Exception) {
                FetchAllResult.Error(e.message ?: "Nieznany błąd połączenia")
            }
        }

    suspend fun fetchDay(webAppUrl: String, secretToken: String, date: String): DayResult =
        withContext(Dispatchers.IO) {
            try {
                val json = request(webAppUrl, secretToken, "mode" to "day", "date" to date)
                if (!json.has("date")) return@withContext DayResult.Error(OUTDATED_SCRIPT)
                DayResult.Success(
                    DayInfo(
                        hours = json.optDouble("hours", 0.0),
                        editUrl = json.optString("editUrl", "")
                    )
                )
            } catch (e: Exception) {
                DayResult.Error(e.message ?: "Nieznany błąd połączenia")
            }
        }

    suspend fun setHours(webAppUrl: String, secretToken: String, date: String, hours: Double): FetchResult =
        withContext(Dispatchers.IO) {
            try {
                val json = request(
                    webAppUrl, secretToken,
                    "mode" to "set", "date" to date, "hours" to hours.toString()
                )
                if (!json.has("dayHours")) return@withContext FetchResult.Error(OUTDATED_SCRIPT)
                FetchResult.Success(parseEarnings(json))
            } catch (e: Exception) {
                FetchResult.Error(e.message ?: "Nieznany błąd połączenia")
            }
        }

    private fun parseEarnings(json: JSONObject) = EarningsData(
        month = json.optString("month", "-"),
        year = json.optInt("year", 0),
        hours = json.optDouble("hours", 0.0),
        gross = json.optDouble("gross", 0.0),
        net = json.optDouble("net", 0.0),
        editUrl = json.optString("editUrl", "")
    )

    private fun request(webAppUrl: String, secretToken: String, vararg params: Pair<String, String>): JSONObject {
        if (webAppUrl.isBlank()) throw IllegalStateException("Brak skonfigurowanego adresu URL")
        val builder = Uri.parse(webAppUrl).buildUpon().appendQueryParameter("token", secretToken)
        params.forEach { (k, v) -> builder.appendQueryParameter(k, v) }
        val body = httpGet(builder.build().toString())
            ?: throw IllegalStateException("Brak odpowiedzi serwera")
        val json = JSONObject(body)
        if (json.has("error")) throw IllegalStateException(json.getString("error"))
        return json
    }

    private fun httpGet(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }
        connection.disconnect()

        if (code !in 200..299) return null
        return body
    }
}
