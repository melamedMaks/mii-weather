package mii.weather.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import mii.weather.apiKey1
import mii.weather.apiKey2
import mii.weather.models.*
import mii.weather.network.WeatherRepository
import mii.weather.network.WeatherRepository.Companion.airPollutionResultUpdated
import mii.weather.network.WeatherRepository.Companion.oneCallWeatherResultUpdated
import mii.weather.network.WeatherRepository.Companion.weatherResultUpdated
import java.net.URL

class CommonViewModel(application: Application) : AndroidViewModel(application) {

    private var _oneCallWeatherResult = MutableLiveData<OneCallWeatherResult>()
    val oneCallWeatherResult: LiveData<OneCallWeatherResult> get() = _oneCallWeatherResult

    private var _airPollutionResult = MutableLiveData<AirPollutionResult>()
    val airPollutionResult: LiveData<AirPollutionResult> get() = _airPollutionResult

    private var _errorHandler = MutableLiveData<Boolean>()
    val errorHandler: LiveData<Boolean> get() = _errorHandler

    private val handler = CoroutineExceptionHandler { _, it ->
        _errorHandler.postValue(true)
        exHandler(application, it)
    }

    fun getWeatherByQuery(query: String, context: Context) {
        viewModelScope.launch(handler) {
            if (WeatherRepository.getFromDataBase(context, query)) {
                getEightWeatherData()
                getairPollutionData()
                WeatherRepository.dataBaseUpdate(context, query)
            } else {
                val weatherResult = WeatherRepository.getWeatherByQuery(query)
                weatherResultUpdated = weatherResult
                val lat = weatherResult.city.coord.lat.toString()
                val lon = weatherResult.city.coord.lon.toString()
                getOneCallWeatherByLatLon(lat, lon, query, context)
                getAirPollutionByLatLon(lat, lon, query, context)
                onLineUiUpdated = true
            }
        }
    }

    fun getWeatherByLatLon(lat: String, lon: String, context: Context) {
        viewModelScope.launch(handler) {
            val weatherResult = WeatherRepository.getWeatherByLatLon(lat, lon)
            weatherResultUpdated = weatherResult
            getAirPollutionByLatLon(lat, lon, "", context)
            getOneCallWeatherByLatLon(lat, lon, "", context)
            onLineUiUpdated = true
        }
    }

    private fun getOneCallWeatherByLatLon(
        lat: String,
        lon: String,
        query: String,
        context: Context
    ) {
        if (lat.isNotEmpty()) {
            _oneCallWeatherResult.apply {
                viewModelScope.launch(handler) {
                    val oneCallWeatherResult =
                        WeatherRepository.getOneCallWeatherByLatLon(lat, lon)
                    oneCallWeatherResultUpdated = oneCallWeatherResult
                    WeatherRepository.dataBaseUpdate(context, query)
                    value = oneCallWeatherResult
                }
            }
        }
    }

    private fun getAirPollutionByLatLon(lat: String, lon: String, query: String, context: Context) {
        _airPollutionResult.apply {
            viewModelScope.launch(handler) {
                val airPollutionResult = WeatherRepository.getAirPollutionByLatLon(lat, lon)
                airPollutionResultUpdated = airPollutionResult
                WeatherRepository.dataBaseUpdate(context, query)
                value = airPollutionResult
            }
        }
    }

    fun getWeatherLastSavedInDB(context: Context) {
        viewModelScope.launch(handler) {
            if (WeatherRepository.getLatestFromDataBase(context)) {
                onLineUiUpdated = false
                getEightWeatherData()
                getairPollutionData()
            }
        }
    }

    fun getEightWeatherData() {
        if (oneCallWeatherResultUpdated != null) {
            _oneCallWeatherResult.apply {
                viewModelScope.launch(handler) {
                    value = oneCallWeatherResultUpdated
                }
            }
        }
    }

    fun getairPollutionData() {
        if (airPollutionResultUpdated != null) {
            _airPollutionResult.apply {
                viewModelScope.launch(handler) {
                    value = airPollutionResultUpdated
                }
            }
        }
    }

    var tileProviderClouds: TileProvider = object : UrlTileProvider(256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
            val url: String = String.format(
                "$cloudsTileUrl$apiKey1",
                zoom, x, y
            )
            try {
                return URL(url)
            } catch (e: Exception) {
                Log.e("Exception", e.message.toString())
            }
            return null
        }
    }
    var tileProviderPrecipitation: TileProvider = object : UrlTileProvider(256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
            val url: String = String.format(
                "$precipitationTileUrl$apiKey1",
                zoom, x, y
            )
            println(url)
            try {
                return URL(url)
            } catch (e: Exception) {
                Log.e("Exception", e.message.toString())
            }
            return null
        }
    }
    var tileProviderWind: TileProvider = object : UrlTileProvider(256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
            val url: String = String.format(
                "$windTileUrl$apiKey2",
                zoom, x, y
            )
            println(url)
            try {
                return URL(url)
            } catch (e: Exception) {
                Log.e("Exception", e.message.toString())
            }
            return null
        }
    }
    var tileProviderTemp: TileProvider = object : UrlTileProvider(256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
            val url: String = String.format(
                "$tempTileUrl$apiKey2",
                zoom, x, y
            )
            println(url)
            try {
                return URL(url)
            } catch (e: Exception) {
                Log.e("Exception", e.message.toString())
            }
            return null
        }
    }

    private fun exHandler(application: Application, it: Throwable) {
        when {
            noNetworkConnection -> {
                errorToast(application.applicationContext, warningInternet)
            }
            it.localizedMessage?.contains("404") == true -> {
                errorToast(application.applicationContext, warningNotFound)
            }
            else -> {
                errorToast(application.applicationContext, warningGeneral)
            }
        }
    }
}
