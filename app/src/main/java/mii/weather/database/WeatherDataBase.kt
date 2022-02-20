package mii.weather.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mii.weather.dao.WeatherDAO
import mii.weather.entities.WeatherData


const val DB_VERSION = 1

const val DB_NAME = "WeatherDB"

@Database(entities = [WeatherData::class], version = DB_VERSION, exportSchema = false)
abstract class WeatherDataBase : RoomDatabase() {

    companion object {
        fun create(context: Context): WeatherDataBase {
            return Room.databaseBuilder(context, WeatherDataBase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    abstract fun weatherDAO(): WeatherDAO
}