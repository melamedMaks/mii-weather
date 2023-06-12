package mii.weather.network

import mii.weather.utils.apiKey2
import mii.weather.models.AirPollutionResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AirPollutionService {
    @GET("forecast")
    suspend fun getAirPollutionByLatLon(
        @Query("lat") lat: String, @Query("lon") lon: String,
        @Query("appid") appid: String = apiKey2
    ): AirPollutionResult

    companion object {
        fun create(): AirPollutionService {
            return Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/air_pollution/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AirPollutionService::class.java)
        }
    }
}