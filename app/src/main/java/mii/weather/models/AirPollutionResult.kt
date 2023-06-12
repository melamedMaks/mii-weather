package mii.weather.models

import com.google.gson.annotations.SerializedName

data class AirPollutionResult(
    val coord: PollutionCoord,
    val list: List<HourlyPollutionList>
)

data class PollutionCoord(
    val lon: Double,
    val lat: Double
)

class HourlyPollutionList(
    val main: PollutionMain,
    val components: Components,
    val dt: Long
)

data class PollutionMain(
    val aqi: Int
)

class Components(
    val co: Double,
    val no: Double,
    @SerializedName("no2")
    val noTwo: Double,
    @SerializedName("o3")
    val oThree: Double,
    @SerializedName("so2")
    val soTwo: Double,
    @SerializedName("pm2_5")
    val pmTwoFive: Double,
    @SerializedName("pm10")
    val pmTen: Double,
    @SerializedName("nh3")
    val nhThree: Double
)