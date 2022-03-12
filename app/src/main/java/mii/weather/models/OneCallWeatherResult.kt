package mii.weather.models

import com.google.gson.annotations.SerializedName

data class OneCallWeatherResult(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    @SerializedName("timezone_offset")
    val timezoneOffset: Long,
    val current: Current,
    val daily: List<DailyList>,
    val hourly: List<HourlyList>,
    val alerts: List<Alerts>?
)

data class Current(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("dew_point")
    val dewPoint: Double,
    val uvi: Double,
    val clouds: Int,
    val visibility: Int,
    @SerializedName("wind_speed")
    val windSpeed: Double,
    @SerializedName("wind_deg")
    val windDeg: Int,
    @SerializedName("wind_gust")
    val windGust: Double,
    val weather: List<CurrentWeather>
)


data class HourlyList(
    val dt: Long,
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("dew_point")
    val dewPoint: Double,
    val uvi: Double,
    val clouds: Long,
    val visibility: Int,
    @SerializedName("wind_speed")
    val windSpeed: Double,
    @SerializedName("wind_deg")
    val windDeg: Int,
    @SerializedName("wind_gust")
    val windGust: Double,
    val weather: List<HourlyWeather>,
    val pop: Double
)


data class DailyList(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val moonrise: Long,
    val moonset: Long,
    @SerializedName("moon_phase")
    val moonPhase: Double,
    val temp: Temp,
    @SerializedName("feels_like")
    val feelsLike: FeelsLike,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("dew_point")
    val dewPoint: Double,
    @SerializedName("wind_speed")
    val windSpeed: Double,
    @SerializedName("wind_deg")
    val windDeg: Int,
    @SerializedName("wind_gust")
    val windGust: Double,
    val weather: List<DailyWeather>,
    val clouds: Long,
    val pop: Double,
    val snow: Double,
    val uvi: Double
)

data class Temp(
    val day: Double,
    val min: Double,
    val max: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

data class FeelsLike(
    val day: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

data class CurrentWeather(
    val id: Double,
    val main: String,
    val description: String,
    val icon: String
)

data class DailyWeather(
    val id: Double,
    val main: String,
    val description: String,
    val icon: String
)

data class HourlyWeather(
    val id: Double,
    val main: String,
    val description: String,
    val icon: String
)

data class Alerts(
    @SerializedName("sender_name")
    val senderName: String,
    val event: String,
    val start: Long,
    val end: Long,
    val description: String,
    val tags: List<String>
)