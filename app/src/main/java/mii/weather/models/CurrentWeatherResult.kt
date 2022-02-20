package mii.weather.models

import com.google.gson.annotations.SerializedName

data class WeatherResult(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<WeatherList>,
    val city: City
)

data class WeatherList(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Long,
    val pop: Double,
    val sys: Sys,
    @SerializedName("dt_txt")
    val dtText: String
)

data class Main(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    val pressure: Double,
    @SerializedName("sea_level")
    val seaLevel: Double,
    @SerializedName("grnd_level")
    val grndLevel: Double,
    val humidity: Int,
    @SerializedName("temp_kf")
    val tempKf: Double
)

data class Weather(
    val id: Double,
    val main: String,
    val description: String,
    val icon: String
)

data class Clouds(
    val all: Double
)

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double
)

data class Sys(
    val pod: String
)

data class City(
    val id: Long,
    val name: String,
    val coord: Coord,
    val country: String,
    val population: Long,
    val timezone: Long,
    val sunrise: Long,
    val sunset: Long
)

data class Coord (
    val lat: Double,
    val lon: Double
    )
