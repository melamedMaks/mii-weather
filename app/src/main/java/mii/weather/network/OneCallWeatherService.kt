package mii.weather.network

import mii.weather.apiKey
import mii.weather.models.OneCallWeatherResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OneCallWeatherService {
    @GET("onecall")
    suspend fun getWeatherDataByLatLon(
        @Query("lat") lat: String, @Query("lon") lon: String,
        @Query("exclude") exclude: String = "minutely",
        @Query("appid") appid: String = apiKey,
        @Query("units") units: String = "metric"
    ): OneCallWeatherResult

    companion object {
        fun create(): OneCallWeatherService {
            return Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OneCallWeatherService::class.java)
        }
    }
}