package com.savings.tracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fix #8/#18/#20: Fetches live USD→INR rate from Frankfurter API.
 * Falls back to cached rate if offline or fetch fails.
 * Rate is cached with a 6-hour TTL so we don't hammer the API.
 */
object ExchangeRateService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val URL = "https://api.frankfurter.dev/v2/rate/USD/INR"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L  // 6 hours

    /**
     * Returns the current USD→INR rate.
     * @param cachedRate  The last known rate from DataStore
     * @param cacheTime   When that rate was fetched
     * @return Pair(rate, isLive) — isLive = false means we're using a cached value
     */
    suspend fun getRate(cachedRate: Double, cacheTime: Long): Pair<Double, Boolean> {
        val age = System.currentTimeMillis() - cacheTime
        if (age < CACHE_TTL_MS && cachedRate > 0) {
            return Pair(cachedRate, false)  // cache still fresh
        }
        return try {
            val rate = fetchLive()
            Pair(rate, true)
        } catch (e: Exception) {
            // Fix #20: Offline fallback — use cached rate, show indicator in UI
            val fallback = if (cachedRate > 0) cachedRate else 83.5
            Pair(fallback, false)
        }
    }

    private suspend fun fetchLive(): Double = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val body = response.body?.string() ?: throw Exception("Empty body")
            JSONObject(body).getDouble("rate")
        }
    }

    /** Convert amount in fromCurrency to toCurrency using given rate (USD→INR) */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String, usdToInr: Double): Double {
        if (fromCurrency == toCurrency) return amount
        return if (fromCurrency == "USD" && toCurrency == "INR") {
            amount * usdToInr
        } else {
            amount / usdToInr
        }
    }
}
