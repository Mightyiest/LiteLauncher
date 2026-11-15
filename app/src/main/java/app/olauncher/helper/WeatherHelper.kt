package app.olauncher.helper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.util.Consumer
import app.olauncher.BuildConfig
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

object WeatherHelper {

    private data class LocationInfo(
        val lat: Double,
        val lon: Double,
        val cityHint: String = "",
    )

    private data class WeatherReading(
        val tempCelsius: Double,
        val condition: String,
    )

    fun hasWeatherLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canFetchWeather(context: Context, prefs: Prefs): Boolean {
        return hasWeatherLocationPermission(context) || prefs.weatherIpFallback
    }

    suspend fun fetchCurrentWeather(context: Context, prefs: Prefs): String? = withContext(Dispatchers.IO) {
        if (!canFetchWeather(context, prefs)) return@withContext null

        val locInfo = resolveLocation(context, prefs) ?: return@withContext null
        val city = locInfo.cityHint
            .ifBlank { resolveCityFromGeocoder(context, locInfo.lat, locInfo.lon) }
            .ifBlank { resolveCityFromNetwork(locInfo.lat, locInfo.lon) }

        val reading = fetchFromMetNorway(locInfo.lat, locInfo.lon)
            ?: fetchFromWttrIn(locInfo.lat, locInfo.lon)
            ?: fetchFromOpenMeteo(locInfo.lat, locInfo.lon)
            ?: return@withContext null

        val isCelsius = prefs.weatherMode != Constants.WeatherMode.FAHRENHEIT
        val displayTemp = if (isCelsius) reading.tempCelsius else (reading.tempCelsius * 9.0 / 5.0 + 32.0)
        val temp = displayTemp.roundToInt()
        val unitSymbol = if (isCelsius) "C" else "F"

        val formatted = if (city.isNotBlank()) {
            "$city \u00B7 ${temp}\u00B0$unitSymbol ${reading.condition}"
        } else {
            "${temp}\u00B0$unitSymbol \u00B7 ${reading.condition}"
        }
        prefs.cachedWeatherText = formatted
        prefs.weatherLastUpdated = System.currentTimeMillis()
        formatted
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveLocation(context: Context, prefs: Prefs): LocationInfo? {
        if (hasWeatherLocationPermission(context)) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var bestLocation: Location? = null

            if (locationManager != null) {
                val coarseProviders = listOf("fused", LocationManager.NETWORK_PROVIDER)
                for (provider in coarseProviders) {
                    val loc = try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    } catch (_: Exception) {
                        null
                    }
                    if (loc != null && (bestLocation == null || loc.time > bestLocation.time)) {
                        bestLocation = loc
                    }
                }

                val isStale = bestLocation == null || (System.currentTimeMillis() - bestLocation.time > 30 * 60_000L)
                if (isStale) {
                    val activeProviders = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isProviderEnabled(locationManager, "fused")) {
                            add("fused")
                        }
                        if (isProviderEnabled(locationManager, LocationManager.NETWORK_PROVIDER)) {
                            add(LocationManager.NETWORK_PROVIDER)
                        }
                    }
                    for (provider in activeProviders) {
                        val fresh = withContext(Dispatchers.Main) {
                            requestSingleLocation(context, locationManager, provider, 3_500L)
                        }
                        if (fresh != null) {
                            bestLocation = fresh
                            break
                        }
                    }
                }
            }

            if (bestLocation != null) {
                prefs.lastWeatherLat = bestLocation.latitude.toFloat()
                prefs.lastWeatherLon = bestLocation.longitude.toFloat()
                return LocationInfo(bestLocation.latitude, bestLocation.longitude)
            }
        }

        val ipLoc = resolveLocationFromIp()
        if (ipLoc != null) {
            prefs.lastWeatherLat = ipLoc.lat.toFloat()
            prefs.lastWeatherLon = ipLoc.lon.toFloat()
            return ipLoc
        }

        if (!prefs.lastWeatherLat.isNaN() && !prefs.lastWeatherLon.isNaN()) {
            return LocationInfo(prefs.lastWeatherLat.toDouble(), prefs.lastWeatherLon.toDouble())
        }

        return null
    }

    private fun isProviderEnabled(locationManager: LocationManager, provider: String): Boolean {
        return try {
            locationManager.isProviderEnabled(provider)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(
        context: Context,
        locationManager: LocationManager,
        provider: String,
        timeoutMs: Long,
    ): Location? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                    Consumer { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                )
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun resolveLocationFromIp(): LocationInfo? {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client?localityLanguage=en"
        val body = httpGetString(url, timeoutMs = 6_000) ?: return null
        return try {
            val json = JSONObject(body)
            val lat = json.optDouble("latitude", Double.NaN)
            val lon = json.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN() || (lat == 0.0 && lon == 0.0)) return null
            val city = json.optNonBlank("city")
                .ifBlank { json.optNonBlank("locality") }
                .ifBlank { json.optNonBlank("principalSubdivision") }
            LocationInfo(lat, lon, city)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromMetNorway(lat: Double, lon: Double): WeatherReading? {
        val url = String.format(
            Locale.US,
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f",
            lat,
            lon
        )
        val body = httpGetString(url, timeoutMs = 6_500) ?: return null
        return try {
            val data = JSONObject(body)
                .getJSONObject("properties")
                .getJSONArray("timeseries")
                .getJSONObject(0)
                .getJSONObject("data")
            val tempC = data.getJSONObject("instant")
                .getJSONObject("details")
                .getDouble("air_temperature")
            val symbolCode = data.optJSONObject("next_1_hours")
                ?.optJSONObject("summary")
                ?.optNonBlank("symbol_code")
                .orEmpty()
                .ifBlank {
                    data.optJSONObject("next_6_hours")
                        ?.optJSONObject("summary")
                        ?.optNonBlank("symbol_code")
                        .orEmpty()
                }
                .ifBlank {
                    data.optJSONObject("next_12_hours")
                        ?.optJSONObject("summary")
                        ?.optNonBlank("symbol_code")
                        .orEmpty()
                }
            val baseSymbol = symbolCode.substringBefore('_').lowercase(Locale.US)
            WeatherReading(tempC, metSymbolToLabel(baseSymbol))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchFromWttrIn(lat: Double, lon: Double): WeatherReading? {
        val url = String.format(Locale.US, "https://wttr.in/%.4f,%.4f?format=j1", lat, lon)
        val body = httpGetString(url, timeoutMs = 6_500) ?: return null
        return try {
            val current = JSONObject(body)
                .getJSONArray("current_condition")
                .getJSONObject(0)
            val tempC = current.getString("temp_C").toDouble()
            val desc = current.optJSONArray("weatherDesc")
                ?.optJSONObject(0)
                ?.optNonBlank("value")
                .orEmpty()
            val condition = if (desc.length in 1..22) {
                desc
            } else {
                wttrDescToLabel(desc, current.optNonBlank("weatherCode"))
            }
            WeatherReading(tempC, condition)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchFromOpenMeteo(lat: Double, lon: Double): WeatherReading? {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&temperature_unit=celsius",
            lat,
            lon
        )
        val body = httpGetString(url, timeoutMs = 4_000) ?: return null
        return try {
            val current = JSONObject(body).getJSONObject("current")
            val tempC = current.getDouble("temperature_2m")
            val code = current.optInt("weather_code", -1)
            WeatherReading(tempC, weatherCodeToLabel(code))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveCityFromGeocoder(context: Context, lat: Double, lon: Double): String {
        return try {
            if (!Geocoder.isPresent()) return ""
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull() ?: return ""
            (addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "").trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun resolveCityFromNetwork(lat: Double, lon: Double): String {
        val url = String.format(
            Locale.US,
            "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%.4f&longitude=%.4f&localityLanguage=en",
            lat,
            lon
        )
        val body = httpGetString(url, timeoutMs = 6_000) ?: return ""
        return try {
            val json = JSONObject(body)
            json.optNonBlank("city")
                .ifBlank { json.optNonBlank("locality") }
                .ifBlank { json.optNonBlank("principalSubdivision") }
        } catch (_: Exception) {
            ""
        }
    }

    private fun JSONObject.optNonBlank(key: String): String {
        return if (isNull(key)) "" else optString(key, "").trim()
    }

    private fun httpGetString(urlString: String, timeoutMs: Int = 6_500): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doInput = true
                setRequestProperty(
                    "User-Agent",
                    "LiteLauncher/${BuildConfig.VERSION_NAME} (https://github.com/Mightyiest/Olauncher)"
                )
                setRequestProperty("Accept", "application/json")
                connect()
            }
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun metSymbolToLabel(symbol: String): String = when {
        symbol == "clearsky" -> "Clear"
        symbol == "fair" -> "Mostly clear"
        symbol == "partlycloudy" -> "Partly cloudy"
        symbol == "cloudy" -> "Cloudy"
        symbol == "fog" -> "Fog"
        "thunder" in symbol -> "Storm"
        "snow" in symbol || "sleet" in symbol -> "Snow"
        "lightrain" in symbol || "drizzle" in symbol -> "Light rain"
        "rain" in symbol -> "Rain"
        else -> "Fair"
    }

    private fun wttrDescToLabel(desc: String, code: String): String {
        val lower = desc.lowercase(Locale.US)
        return when {
            "thunder" in lower || "storm" in lower -> "Storm"
            "snow" in lower || "sleet" in lower || "blizzard" in lower -> "Snow"
            "drizzle" in lower || "light rain" in lower -> "Light rain"
            "rain" in lower || "shower" in lower -> "Rain"
            "fog" in lower || "mist" in lower -> "Fog"
            "partly" in lower -> "Partly cloudy"
            "cloud" in lower || "overcast" in lower -> "Cloudy"
            "clear" in lower || "sunny" in lower -> "Clear"
            code == "113" -> "Clear"
            code == "116" -> "Partly cloudy"
            code in setOf("119", "122") -> "Cloudy"
            code in setOf("143", "248", "260") -> "Fog"
            else -> "Fair"
        }
    }

    private fun weatherCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rain"
        in 71..77, 85, 86 -> "Snow"
        in 95..99 -> "Storm"
        else -> "Fair"
    }
}
