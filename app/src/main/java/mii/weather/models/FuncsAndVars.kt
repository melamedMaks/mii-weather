package mii.weather.models

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes


import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.picasso.Picasso
import mii.weather.R

import java.util.*
import kotlin.math.roundToInt

var onLineUiUpdated = false
var noNetworkConnection = true

@StringRes
val TAB_TITLES = arrayOf(
    R.string.current_weather,
    R.string.later_weather,
    R.string.eight_days_weather
)

const val warningInternet = "NO INTERNET CONNECTION"
const val warningLocation = "CHECK LOCATION STATUS"
const val warningNotFound = "NOT FOUND"
const val warningCheckLocationPermission = "CHECK LOCATION PERMISSION"
const val warningGeneral = "Something went wrong, try again later."

const val cloudsTileUrl = "https://tile.openweathermap.org/map/clouds_new/%d/%d/%d.png?appid="
const val precipitationTileUrl =
    "https://tile.openweathermap.org/map/precipitation_new/%d/%d/%d.png?appid=\n"
const val windTileUrl = "https://tile.openweathermap.org/map/wind_new/%d/%d/%d.png?appid="
const val tempTileUrl = "https://tile.openweathermap.org/map/temp_new/%d/%d/%d.png?appid="

fun errorToast(context: Context, message: String?) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun getLocalDateFromTimeStamp(utc: Long, timezone: Long): String {
    val date = getDateFromTimeStamp(utc, timezone)
    return getFormattedDate(date, "EEE, d MMM yyyy")
}

private fun getFormattedDate(date: Date, pattern: String): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
}

fun getLocalHourFromTimeStamp(utc: Long, timezone: Long): String {
    val date = getDateFromTimeStamp(utc, timezone)
    return getFormattedDate(date, "HH:mm")
}

private fun getDateFromTimeStamp(utc: Long, timezone: Long): Date {
    val localUTC = (utc + timezone) * 1000
    return Date(localUTC)
}

fun setIconFromUrl(
    icon: String,
    view: ImageView
) {
    Picasso.get().load("https://openweathermap.org/img/wn/$icon@2x.png").resize(800, 800)
        .into(view)
}

fun tempToIntAndString(temp: Double): String {
    val celsius = "°"
    val t = temp.roundToInt()
    return when {
        t > 0 -> {
            "+${t}$celsius"
        }
        else -> {
            "${t}$celsius"
        }
    }
}

fun firstCharUp(letter: String): String {
    return letter.replaceFirstChar { it.uppercase() }
}

//moshi library init
val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val jsonAdapterCurrent: JsonAdapter<WeatherResult> = moshi.adapter(WeatherResult::class.java)
val JSON_ADAPTER_ONE_CALL: JsonAdapter<OneCallWeatherResult> =
    moshi.adapter(OneCallWeatherResult::class.java)


//moshi method
fun toJsonCurrentWeather(weatherResult: WeatherResult): String {
    return jsonAdapterCurrent.toJson(weatherResult)
}

fun toJsonOneCallWeather(oneCallWeatherResult: OneCallWeatherResult?): String {
    return JSON_ADAPTER_ONE_CALL.toJson(oneCallWeatherResult)
}

//moshi method
fun fromJsonCurrent(json: String): WeatherResult? {
    return jsonAdapterCurrent.fromJson(json)
}

fun fromJsonOneCall(json: String): OneCallWeatherResult? {
    return JSON_ADAPTER_ONE_CALL.fromJson(json)
}


fun getCurrentUnix(): Long {
    return System.currentTimeMillis() / 1000L
}

fun unixDiff(previous: Long?, current: Long): Boolean {
    if (previous != null) {
        return previous >= current
    }
    return false
}

