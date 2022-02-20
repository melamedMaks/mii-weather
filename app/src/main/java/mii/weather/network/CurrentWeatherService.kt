package mii.weather.network

import mii.weather.apiKey
import mii.weather.models.WeatherResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrentWeatherService {
    @GET("forecast")
    suspend fun getWeatherDataByLatLon(
        @Query("lat") lat: String, @Query("lon") lon: String,
        @Query("appid") appid: String = apiKey,
        @Query("units") units: String = "metric"
    ): WeatherResult

    @GET("forecast")
    suspend fun getWeatherDataByCityName(
        @Query("q") q: String,
        @Query("appid") appid: String = apiKey,
        @Query("units") units: String = "metric"
    ): WeatherResult

    companion object {
        fun create(): CurrentWeatherService {
            return Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CurrentWeatherService::class.java)
        }
    }
}