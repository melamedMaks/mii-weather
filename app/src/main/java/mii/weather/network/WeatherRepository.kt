package mii.weather.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mii.weather.database.WeatherDataBase
import mii.weather.entities.WeatherData
import mii.weather.models.*

class WeatherRepository {
    companion object {
        lateinit var weatherResultUpdated: WeatherResult
        var oneCallWeatherResultUpdated: OneCallWeatherResult? = null
        var airPollutionResultUpdated: AirPollutionResult? = null
        lateinit var cityCountryCommonUse: String


        suspend fun getWeatherByLatLon(
            lat: String,
            lon: String
        ): WeatherResult {
            return withContext(Dispatchers.IO) {
                val weatherResult = CurrentWeatherService.create().getWeatherDataByLatLon(lat, lon)
                cityCountryCommonUse = "${weatherResult.city.name}, ${weatherResult.city.country}"
                weatherResult
            }
        }

        suspend fun getAirPollutionByLatLon(
            lat: String,
            lon: String
        ): AirPollutionResult {
            return withContext(Dispatchers.IO) {
                val airPollutionResult = AirPollutionService.create()
                    .getAirPollutionByLatLon(lat, lon)
                airPollutionResult
            }
        }

        suspend fun getOneCallWeatherByLatLon(
            lat: String,
            lon: String
        ): OneCallWeatherResult {
            return withContext(Dispatchers.IO) {
                val oneCallWeatherResult = OneCallWeatherService.create()
                    .getWeatherDataByLatLon(lat, lon)
                oneCallWeatherResult
            }
        }

        suspend fun getWeatherByQuery(q: String): WeatherResult {

            return withContext(Dispatchers.IO) {
                val weatherResult = CurrentWeatherService.create().getWeatherDataByCityName(q)
                cityCountryCommonUse = "${weatherResult.city.name}, ${weatherResult.city.country}"
                weatherResult
            }
        }

        suspend fun getFromDataBase(context: Context, query: String): Boolean {
            val db = WeatherDataBase.create(context)
            val weatherResultByQuery = db.weatherDAO().getWeather()
            val cityName = query.lowercase()

            for (weatherData in weatherResultByQuery) {
                val id = weatherData.id.split("@")
                if (!unixDiff(weatherData.date, getCurrentUnix())) {
                    println(getCurrentUnix())
                    db.weatherDAO().deleteById(weatherData.id)
                }
                if ((id[0] == cityName || (id.size > 1 && id[1] == cityName)) && unixDiff(weatherData.date, getCurrentUnix())) {
                    weatherResultUpdated =
                        fromJsonCurrent(weatherData.weatherResultJson)!!
                    cityCountryCommonUse =
                        "${weatherResultUpdated.city.name}, ${weatherResultUpdated.city.country}"
                    oneCallWeatherResultUpdated =
                        fromJsonOneCall(weatherData.oneCallWeatherResultJson)
                    airPollutionResultUpdated =
                        fromJsonAirPollution(weatherData.airPollutionResultJson)
                    return true
                }
            }
            return false
        }

        suspend fun getLatestFromDataBase(context: Context): Boolean {
            val db = WeatherDataBase.create(context)
            val weatherResult = db.weatherDAO().getWeather()
            if (weatherResult.isNotEmpty()) {
                val index = weatherResult.size - 1
                weatherResultUpdated = fromJsonCurrent(weatherResult[index].weatherResultJson)!!
                cityCountryCommonUse =
                    "${weatherResultUpdated.city.name}, ${weatherResultUpdated.city.country}"
                oneCallWeatherResultUpdated =
                    fromJsonOneCall(weatherResult[index].oneCallWeatherResultJson)
                airPollutionResultUpdated = fromJsonAirPollution(weatherResult[index].airPollutionResultJson)
                return true
            }
            return false
        }

        suspend fun dataBaseUpdate(context: Context, query: String) {
            val db = WeatherDataBase.create(context)
            val cityName = weatherResultUpdated.city.name
            val date =
                oneCallWeatherResultUpdated?.hourly?.get(1)?.dt //takes one hour ahead, to prevent redundant api calls
            val id: String = if (query.isEmpty()) {
                cityName.lowercase()
            } else {
                "${cityName.lowercase()}@$query"
            }

            val weatherData = WeatherData(
                id = id, toJsonCurrentWeather(weatherResultUpdated),
                toJsonOneCallWeather(oneCallWeatherResultUpdated), toJsonAirPollution(
                    airPollutionResultUpdated), date
            )

           db.weatherDAO().addWeather(weatherData)
        }
    }
}