package mii.weather.adapters

import android.graphics.Color.rgb
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mii.weather.databinding.LayoutLaterItemBinding
import mii.weather.models.*
import mii.weather.network.WeatherRepository

class LaterTodayAdapter(private val laterWeatherResult: OneCallWeatherResult, private val airPollutionResult: AirPollutionResult) :
    RecyclerView.Adapter<LaterTodayAdapter.VH>() {

    private val timeZone = laterWeatherResult.timezoneOffset

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
            LayoutLaterItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: VH, position: Int) {
        with(airPollutionResult.list[position]){
            if (position < 120){
                val aqi = aqiCalculation(components)
                var color:Int = rgb(255, 255, 255)
                var text = "AQI"
                val pair = getAqiTextColor(aqi, text, color)
                color = pair.first
                text = pair.second
                holder.binding.airPollutionLaterValue.setBackgroundColor(color)
                holder.binding.airPollutionLaterValue.text = text
            }
        }
        with(laterWeatherResult.hourly[position]) {

            holder.binding.textViewDateLater.text =
                getLocalDateFromTimeStamp(dt, timeZone) //help fun

            holder.binding.textViewHourLaterValue.text =
                getLocalHourFromTimeStamp(dt, timeZone) //help fun

            holder.binding.textViewCityCountryLater.text = WeatherRepository.cityCountryCommonUse

            setIconFromUrl(weather[0].icon, holder.binding.imageViewIconLater) //help fun

            holder.binding.textViewTempLaterText.text = tempToIntAndString(temp)

            holder.binding.textViewDescriptionLaterText.text = firstCharUp(weather[0].description)

            val visibility = "Visibility: ${visibility / 1000f} km"
            holder.binding.textViewVisibilityLaterValue.text = visibility

            val uv = "$uvi / 10"
            holder.binding.textViewUvLaterValue.text = uv

            val feelsLikeHourly = "Feels like: ${tempToIntAndString(feelsLike)}"
            holder.binding.textViewFeelsLikeLater.text = feelsLikeHourly

            val wind = "${windSpeed} m/s"
            holder.binding.textViewWindValueLater.text = wind

            val humidityLater = "${humidity}%"
            holder.binding.textViewHumidityLaterValue.text = humidityLater

            val pressureLater = "${pressure} hPa"
            holder.binding.textViewPressureLaterValue.text = pressureLater

        }
    }

    override fun getItemCount(): Int = laterWeatherResult.hourly.size

    inner class VH(val binding: LayoutLaterItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}