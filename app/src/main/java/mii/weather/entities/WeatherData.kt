package mii.weather.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Weather")
data class WeatherData(
    @PrimaryKey
    val id: String,
    val weatherResultJson: String,
    val oneCallWeatherResultJson: String,
    val date: Long?
)