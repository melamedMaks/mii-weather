package mii.weather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mii.weather.databinding.LayoutLaterItemBinding
import mii.weather.models.*
import mii.weather.network.WeatherRepository

class LaterTodayAdapter(private val oneCallWeatherResult: OneCallWeatherResult) :
    RecyclerView.Adapter<LaterTodayAdapter.VH>() {

    private val timeZone = oneCallWeatherResult.timezoneOffset

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutLaterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))


    override fun onBindViewHolder(holder: VH, position: Int) {
        with(oneCallWeatherResult.hourly[position]) {

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

            val uv = "$uvi of 10"
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

    override fun getItemCount(): Int = oneCallWeatherResult.hourly.size

    inner class VH(val binding: LayoutLaterItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}