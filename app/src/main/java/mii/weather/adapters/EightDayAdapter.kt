package mii.weather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mii.weather.databinding.LayoutEightItemBinding
import mii.weather.models.*
import mii.weather.network.WeatherRepository

class EightDayAdapter(private val oneCallWeatherResult: OneCallWeatherResult) :
    RecyclerView.Adapter<EightDayAdapter.VH>() {

    private val timeZone = oneCallWeatherResult.timezoneOffset
    private val city = WeatherRepository.weatherResultUpdated.city.name
    private val country = WeatherRepository.weatherResultUpdated.city.country

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        LayoutEightItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        with(oneCallWeatherResult.daily[position]) {

            holder.binding.textViewDateEightValue.text =
                getLocalDateFromTimeStamp(dt, timeZone) //help fun

            val place = "$city, $country"
            holder.binding.textViewCityCountryEight.text = place

            holder.binding.textViewTempEightValue.text = tempToIntAndString(temp.day)

            holder.binding.textViewMinTempEight.text = tempToIntAndString(temp.min)
            holder.binding.textViewMaxTempEight.text = tempToIntAndString(temp.max)

            holder.binding.textViewSunsetEightValue.text =
                getLocalHourFromTimeStamp(sunset, timeZone)
            val icon = weather[0].icon

            setIconFromUrl(icon, holder.binding.imageViewIconEight)
            holder.binding.textViewSunriseEightValue.text =
                getLocalHourFromTimeStamp(sunrise, timeZone)

            val morningTemp = "Morn: ${tempToIntAndString(temp.morn)}"
            holder.binding.textViewMorTempEightValue.text = morningTemp

            val dayTemp = "Day: ${tempToIntAndString(temp.day)}"
            holder.binding.textViewDayTempEightValue.text = dayTemp

            val eveningTemp = "Eve: ${tempToIntAndString(temp.eve)}"
            holder.binding.textViewEveTempEightValue.text = eveningTemp

            val nightTemp = "Night: ${tempToIntAndString(temp.night)}"
            holder.binding.textViewNightTempEightValue.text = nightTemp

            val humidity = "$humidity%"
            holder.binding.textViewHumidityEightValue.text = humidity
            val wind = windFormatter(windSpeed)
            holder.binding.textViewWindValueEight.text = wind
            holder.binding.eightWindArrow.animate().rotation(windDeg.toFloat())

            holder.binding.textViewUvEightValue.text = uvi.toString()
            holder.binding.textViewDescriptionEightValue.text = firstCharUp(weather[0].description)
            val pressure = "$pressure hPa"
            holder.binding.textViewPressureEightValue.text = pressure

            val moonset = getLocalHourFromTimeStamp(moonset, timeZone)
            holder.binding.textViewMoonSetEightValue.text = moonset

            holder.binding.textViewMoonPhaseEightValue.text = moonPhase.toString()

            val moonrise = getLocalHourFromTimeStamp(moonrise, timeZone)
            holder.binding.textViewMoonRiseEightValue.text = moonrise
        }
    }

    override fun getItemCount(): Int = 8

    inner class VH(val binding: LayoutEightItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}