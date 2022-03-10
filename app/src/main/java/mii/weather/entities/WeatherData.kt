package mii.weather.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import mii.weather.models.AirPollutionResult

@Entity(tableName = "Weather")
data class WeatherData(
    @PrimaryKey
    val id: String,
    val weatherResultJson: String,
    val oneCallWeatherResultJson: String,
    val airPollutionResult: String,
    val date: Long?
)