package com.example.stormview

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class WeatherWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, R.layout.widget_small)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            layoutId: Int
        ) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val lat = location?.latitude ?: 42.3356
                    val lon = location?.longitude ?: -88.2590
                    fetchAndUpdate(context, appWidgetManager, appWidgetId, layoutId, lat, lon)
                }
            } catch (e: SecurityException) {
                fetchAndUpdate(context, appWidgetManager, appWidgetId, layoutId, 42.3356, -88.2590)
            }
        }

        private fun fetchAndUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            layoutId: Int,
            lat: Double,
            lon: Double
        ) {
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {
                try {
                    val url = "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$lat&longitude=$lon" +
                            "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                            "weather_code,wind_speed_10m,uv_index" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
                            "precipitation_probability_max" +
                            "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=7"

                    val response = URL(url).readText()
                    val json = JSONObject(response)
                    val current = json.getJSONObject("current")
                    val daily = json.getJSONObject("daily")

                    val temp = current.getDouble("temperature_2m").roundToInt()
                    val feelsLike = current.getDouble("apparent_temperature").roundToInt()
                    val humidity = current.getInt("relative_humidity_2m")
                    val windSpeed = current.getDouble("wind_speed_10m").roundToInt()
                    val weatherCode = current.getInt("weather_code")
                    val uvIndex = current.optDouble("uv_index", 0.0).roundToInt()

                    val emoji = getWeatherEmoji(weatherCode)
                    val condition = getWeatherCondition(weatherCode)
                    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

                    // Parse 7-day forecast arrays
                    val dates = daily.getJSONArray("time")
                    val codes = daily.getJSONArray("weather_code")
                    val highs = daily.getJSONArray("temperature_2m_max")
                    val lows = daily.getJSONArray("temperature_2m_min")
                    val precips = daily.getJSONArray("precipitation_probability_max")

                    // Build day name from date string e.g. "2026-02-20"
                    val dayNames = (0 until 7).map { i ->
                        val dateStr = dates.getString(i)
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = sdf.parse(dateStr)
                        if (i == 0) "Today"
                        else SimpleDateFormat("EEE", Locale.getDefault()).format(date!!)
                    }

                    handler.post {
                        val views = RemoteViews(context.packageName, layoutId)

                        // Small widget fields
                        if (layoutId == R.layout.widget_small) {
                            views.setTextViewText(R.id.widget_emoji, emoji)
                            views.setTextViewText(R.id.widget_temp, "$temp°F")
                            views.setTextViewText(R.id.widget_condition, condition)
                            views.setTextViewText(R.id.widget_wind, "💨 $windSpeed mph")
                            views.setTextViewText(R.id.widget_humidity, "💧 $humidity%")
                        }

                        // Medium widget fields
                        if (layoutId == R.layout.widget_medium) {
                            views.setTextViewText(R.id.widget_emoji, emoji)
                            views.setTextViewText(R.id.widget_temp, "$temp°F")
                            views.setTextViewText(R.id.widget_condition, condition)
                            views.setTextViewText(R.id.widget_feels_like, "Feels like $feelsLike°F")
                            views.setTextViewText(R.id.widget_wind, "💨 $windSpeed mph")
                            views.setTextViewText(R.id.widget_humidity, "💧 $humidity%")
                            views.setTextViewText(R.id.widget_uv, "☀️ UV $uvIndex")
                            views.setTextViewText(R.id.widget_updated, "Updated $timeStr")
                        }

                        // Large widget fields - current + 7 day forecast
                        if (layoutId == R.layout.widget_large) {
                            views.setTextViewText(R.id.widget_emoji, emoji)
                            views.setTextViewText(R.id.widget_temp, "$temp°F")
                            views.setTextViewText(R.id.widget_condition, condition)
                            views.setTextViewText(R.id.widget_feels_like, "🌡️ Feels $feelsLike°F")
                            views.setTextViewText(R.id.widget_wind, "💨 $windSpeed mph")
                            views.setTextViewText(R.id.widget_humidity, "💧 $humidity%")
                            views.setTextViewText(R.id.widget_uv, "☀️ UV $uvIndex")
                            views.setTextViewText(R.id.widget_updated, "Updated $timeStr")

                            // Forecast day IDs paired up
                            val dayIds = listOf(
                                Triple(R.id.day1_name, R.id.day1_emoji, Triple(R.id.day1_high, R.id.day1_low, R.id.day1_precip)),
                                Triple(R.id.day2_name, R.id.day2_emoji, Triple(R.id.day2_high, R.id.day2_low, R.id.day2_precip)),
                                Triple(R.id.day3_name, R.id.day3_emoji, Triple(R.id.day3_high, R.id.day3_low, R.id.day3_precip)),
                                Triple(R.id.day4_name, R.id.day4_emoji, Triple(R.id.day4_high, R.id.day4_low, R.id.day4_precip)),
                                Triple(R.id.day5_name, R.id.day5_emoji, Triple(R.id.day5_high, R.id.day5_low, R.id.day5_precip)),
                                Triple(R.id.day6_name, R.id.day6_emoji, Triple(R.id.day6_high, R.id.day6_low, R.id.day6_precip)),
                                Triple(R.id.day7_name, R.id.day7_emoji, Triple(R.id.day7_high, R.id.day7_low, R.id.day7_precip))
                            )

                            for (i in 0 until 7) {
                                val (nameId, emojiId, rest) = dayIds[i]
                                val (highId, lowId, precipId) = rest
                                val high = highs.getDouble(i).roundToInt()
                                val low = lows.getDouble(i).roundToInt()
                                val precip = precips.optInt(i, 0)
                                val dayEmoji = getWeatherEmoji(codes.getInt(i))

                                views.setTextViewText(nameId, dayNames[i])
                                views.setTextViewText(emojiId, dayEmoji)
                                views.setTextViewText(highId, "$high°")
                                views.setTextViewText(lowId, "$low°")
                                views.setTextViewText(precipId, "💧 $precip%")
                            }
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    handler.post {
                        val views = RemoteViews(context.packageName, layoutId)
                        views.setTextViewText(R.id.widget_temp, "⚠️")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        private fun getWeatherEmoji(code: Int): String {
            return when (code) {
                0 -> "☀️"
                1 -> "🌤"
                2 -> "⛅"
                3 -> "☁️"
                45, 48 -> "🌫️"
                51, 53, 55 -> "🌦️"
                61, 63, 65 -> "🌧️"
                66, 67 -> "🌨️"
                71, 73, 75 -> "❄️"
                77 -> "🌨️"
                80, 81, 82 -> "🌧️"
                85, 86 -> "❄️"
                95 -> "⛈️"
                96, 99 -> "⛈️"
                else -> "🌡️"
            }
        }

        private fun getWeatherCondition(code: Int): String {
            return when (code) {
                0 -> "Clear Sky"
                1 -> "Mostly Clear"
                2 -> "Partly Cloudy"
                3 -> "Overcast"
                45, 48 -> "Foggy"
                51, 53, 55 -> "Drizzle"
                61, 63, 65 -> "Rain"
                66, 67 -> "Freezing Rain"
                71, 73, 75 -> "Snow"
                77 -> "Snow Grains"
                80, 81, 82 -> "Rain Showers"
                85, 86 -> "Snow Showers"
                95 -> "Thunderstorm"
                96, 99 -> "Thunderstorm w/ Hail"
                else -> "Unknown"
            }
        }
    }
}