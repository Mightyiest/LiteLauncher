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

    fun hasWeatherLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun fetchCurrentWeather(context: Context, prefs: Prefs): String? = withContext(Dispatchers.IO) {
        if (!hasWeatherLocationPermission(context)) return@withContext null

        val coords = resolveCoordinates(context, prefs) ?: return@withContext null
        val lat = coords.first
        val lon = coords.second

        val city = resolveCityFromGeocoder(context, lat, lon)
            .ifBlank { resolveCityFromNetwork(lat, lon) }

        val isCelsius = prefs.weatherMode != Constants.WeatherMode.FAHRENHEIT
        val unitParam = if (isCelsius) "celsius" else "fahrenheit"
        val unitSymbol = if (isCelsius) "C" else "F"

        val weatherUrl =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&temperature_unit=$unitParam"
        val body = httpGetString(weatherUrl) ?: return@withContext null

        try {
            val current = JSONObject(body).getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val code = current.optInt("weather_code", -1)
            val condition = weatherCodeToLabel(code)

            val formatted = if (city.isNotBlank()) {
                "$city \u00B7 ${temp}\u00B0$unitSymbol $condition"
            } else {
                "${temp}\u00B0$unitSymbol \u00B7 $condition"
            }
            prefs.cachedWeatherText = formatted
            prefs.weatherLastUpdated = System.currentTimeMillis()
            formatted
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun resolveCoordinates(context: Context, prefs: Prefs): Pair<Double, Double>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var bestLocation: Location? = null

        if (locationManager != null) {
            val providers = listOf(
                "fused",
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
                LocationManager.GPS_PROVIDER
            )
            for (provider in providers) {
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
                val activeProvider = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isProviderEnabled(locationManager, "fused") -> "fused"
                    isProviderEnabled(locationManager, LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    isProviderEnabled(locationManager, LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> null
                }
                if (activeProvider != null) {
                    val fresh = withContext(Dispatchers.Main) {
                        requestSingleLocation(context, locationManager, activeProvider)
                    }
                    if (fresh != null) bestLocation = fresh
                }
            }
        }

        if (bestLocation != null) {
            prefs.lastWeatherLat = bestLocation.latitude.toFloat()
            prefs.lastWeatherLon = bestLocation.longitude.toFloat()
            return Pair(bestLocation.latitude, bestLocation.longitude)
        }

        if (!prefs.lastWeatherLat.isNaN() && !prefs.lastWeatherLon.isNaN()) {
            return Pair(prefs.lastWeatherLat.toDouble(), prefs.lastWeatherLon.toDouble())
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
    ): Location? = withTimeoutOrNull(8_000L) {
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
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=en"
        val body = httpGetString(url) ?: return ""
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

    private fun httpGetString(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                doInput = true
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
