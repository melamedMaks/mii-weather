package mii.weather.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mii.weather.entities.WeatherData

@Dao
interface WeatherDAO {
    @Query(value = "SELECT * FROM Weather")
    suspend fun getWeather(): List<WeatherData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWeather(weatherData: WeatherData)

    @Query("DELETE FROM Weather WHERE id = :id")
    suspend fun deleteById(id: String)
}