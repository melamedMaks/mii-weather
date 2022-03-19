package mii.weather.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import mii.weather.databinding.FragmentAlertsBinding
import mii.weather.models.Alerts
import mii.weather.models.getLocalDateFromTimeStamp
import mii.weather.network.WeatherRepository
import mii.weather.ui.CommonViewModel

class AlertsFragment: Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commonViewModel: CommonViewModel
    private var callback: ClickListener? = null

    fun setCallback(callback: ClickListener) {
        this.callback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commonViewModel = ViewModelProvider(this)[CommonViewModel::class.java]
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commonViewModel.oneCallWeatherResult.observe(viewLifecycleOwner) {
            it.alerts?.let { alerts -> fetchDataAndRenderAlertFragmentUI(alerts) }
        }

        binding.buttonClose.setOnClickListener {
            callback?.onClickAlertsFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        commonViewModel.getEightWeatherData()
    }

    private fun fetchDataAndRenderAlertFragmentUI(alerts: List<Alerts>) {
        val timezoneOffset = WeatherRepository.oneCallWeatherResultUpdated?.timezoneOffset
        alerts[0].apply {
            binding.textViewAlertEvent.text = this.event

            val startDate =
                "${timezoneOffset?.let { getLocalDateFromTimeStamp(this.start, it) }} - "
            binding.textViewAlertStart.text = startDate

            val endDate = timezoneOffset?.let { getLocalDateFromTimeStamp(this.end, it) }
            binding.textViewAlertEnd.text = endDate

            binding.textViewAlertDescription.text = this.description
            binding.textViewSender.text = this.senderName
        }
    }

    interface ClickListener {
        fun onClickAlertsFragment()
    }
}