package mii.weather.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mii.weather.dao.WeatherDAO
import mii.weather.entities.WeatherData


const val DB_VERSION = 5 // production 5

const val DB_NAME = "WeatherDB"

@Database(entities = [WeatherData::class], version = DB_VERSION)
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