package mii.weather.utils


import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.location.Location
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.picasso.Picasso
import mii.weather.R
import mii.weather.models.AirPollutionResult
import mii.weather.models.Components
import mii.weather.models.OneCallWeatherResult
import mii.weather.models.WeatherResult
import java.util.*
import kotlin.math.roundToInt

var onLineUiUpdated = false
var isNetworkAvailable = false
var timeRange: Long = 3600 * 1000
var lastSavedTimestamp: Long = 0
var latLocal: Double = 0.0
var lonLocal: Double = 0.0
var inProgress = false
var isUIUpdatedByCurrentLocation = false
var alertVisible = false

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
            "${t}$celsius"
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
val JSON_ADAPTER_AIR_POLLUTION: JsonAdapter<AirPollutionResult> =
    moshi.adapter(AirPollutionResult::class.java)


//moshi method
fun toJsonCurrentWeather(weatherResult: WeatherResult): String {
    return jsonAdapterCurrent.toJson(weatherResult)
}

fun toJsonOneCallWeather(oneCallWeatherResult: OneCallWeatherResult?): String {
    return JSON_ADAPTER_ONE_CALL.toJson(oneCallWeatherResult)
}

fun toJsonAirPollution(airPollutionResult: AirPollutionResult?): String {
    return JSON_ADAPTER_AIR_POLLUTION.toJson(airPollutionResult)
}

//moshi method
fun fromJsonCurrent(json: String): WeatherResult? {
    return jsonAdapterCurrent.fromJson(json)
}

fun fromJsonOneCall(json: String): OneCallWeatherResult? {
    return JSON_ADAPTER_ONE_CALL.fromJson(json)
}

fun fromJsonAirPollution(json: String): AirPollutionResult? {
    return JSON_ADAPTER_AIR_POLLUTION.fromJson(json)
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

fun getAqiTextColor(
    airPollutionValue: Int,
    text: String?,
    color: Int
): Pair<Int, String> {
    var textValue = text
    var colorValue = color
    when (airPollutionValue) {
        1 -> {
            textValue = "Very Good"
            colorValue = Color.rgb(50, 174, 211)
        }
        2 -> {
            textValue = "Good"
            colorValue = Color.rgb(153, 185, 101)
        }
        3 -> {
            textValue = "Fair"
            colorValue = Color.rgb(255, 210, 54)
        }
        4 -> {
            textValue = "Poor"
            colorValue = Color.rgb(236, 120, 59)
        }
        5 -> {
            textValue = "Very Poor"
            colorValue = Color.rgb(120, 45, 73)
        }
        6 -> {
            textValue = "Hazardous"
            colorValue = Color.rgb(208, 71, 48)
        }
        else -> textValue = ""
    }
    return Pair(colorValue, textValue)
}

//first pair of Pair is an "Index: (I_low - I_high)"
//second pair of Pair is "Pollutant (hourly) concentration: (C_low - C_high)"
fun aqiCalculation(components: Components): Int {
    val noTwo: Pair<Pair<Int, Int>, Pair<Int, Int>> = when {
        components.noTwo <= 50 -> {
            Pair(Pair(0, 25), Pair(0, 50))
        }
        50 < components.noTwo && components.noTwo <= 100 -> {
            Pair(Pair(26, 50), Pair(51, 100))
        }
        100 < components.noTwo && components.noTwo <= 200 -> {
            Pair(Pair(51, 75), Pair(101, 200))
        }
        200 < components.noTwo && components.noTwo <= 400 -> {
            Pair(Pair(76, 100), Pair(201, 400))
        }
        else -> Pair(Pair(101, 125), Pair(401, 800))
    }

    val pmTen: Pair<Pair<Int, Int>, Pair<Int, Int>> = when {
        components.pmTen <= 25 -> {
            Pair(Pair(0, 25), Pair(0, 25))
        }
        25 < components.pmTen && components.pmTen <= 50 -> {
            Pair(Pair(26, 50), Pair(26, 50))
        }
        50 < components.pmTen && components.pmTen <= 90 -> {
            Pair(Pair(51, 75), Pair(51, 90))
        }
        90 < components.pmTen && components.pmTen <= 180 -> {
            Pair(Pair(76, 100), Pair(91, 180))
        }
        else -> Pair(Pair(101, 125), Pair(181, 360))
    }

    val oThree: Pair<Pair<Int, Int>, Pair<Int, Int>> = when {
        components.oThree <= 60 -> {
            Pair(Pair(0, 25), Pair(0, 60))
        }
        60 < components.oThree && components.oThree <= 120 -> {
            Pair(Pair(26, 50), Pair(61, 120))
        }
        120 < components.oThree && components.oThree <= 180 -> {
            Pair(Pair(51, 75), Pair(121, 180))
        }
        180 < components.oThree && components.oThree <= 240 -> {
            Pair(Pair(76, 100), Pair(181, 240))
        }
        else -> Pair(Pair(101, 125), Pair(241, 300))
    }

    val pmTwoFive: Pair<Pair<Int, Int>, Pair<Int, Int>> = when {
        components.pmTwoFive <= 15 -> {
            Pair(Pair(0, 25), Pair(0, 15))
        }
        15 < components.pmTwoFive && components.pmTwoFive <= 30 -> {
            Pair(Pair(26, 50), Pair(16, 30))
        }
        30 < components.pmTwoFive && components.pmTwoFive <= 55 -> {
            Pair(Pair(51, 75), Pair(31, 55))
        }
        55 < components.pmTwoFive && components.pmTwoFive <= 110 -> {
            Pair(Pair(76, 100), Pair(56, 110))
        }
        else -> Pair(Pair(101, 125), Pair(111, 220))
    }

    val noTwoIndex =
        ((((noTwo.first.second - noTwo.first.first) / (noTwo.second.second - noTwo.second.first)) * (components.noTwo - noTwo.second.first)) + noTwo.first.first).toInt()
    val pmTenIndex =
        ((((pmTen.first.second - pmTen.first.first) / (pmTen.second.second - pmTen.second.first)) * (components.pmTen - pmTen.second.first)) + pmTen.first.first).toInt()
    val oThreeIndex =
        ((((oThree.first.second - oThree.first.first) / (oThree.second.second - oThree.second.first)) * (components.oThree - oThree.second.first)) + oThree.first.first).toInt()
    val pmTwoFiveIndex =
        ((((pmTwoFive.first.second - pmTwoFive.first.first) / (pmTwoFive.second.second - pmTwoFive.second.first)) * (components.pmTwoFive - pmTwoFive.second.first)) + pmTwoFive.first.first).toInt()


    val indexArray = arrayOf(noTwoIndex, pmTenIndex, oThreeIndex, pmTwoFiveIndex)
    indexArray.sort()
    val aqi = when (indexArray[indexArray.size - 1]) {
        in 0..25 -> 1
        in 26..50 -> 2
        in 51..75 -> 3
        in 76..100 -> 4
        in 101..150 -> 5
        else -> {
            6
        }
    }
    return aqi
}

fun windFormatter(wind: Double): String{
    return "${String.format("%.1f", wind)} m/s"
}

