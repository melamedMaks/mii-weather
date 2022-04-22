package mii.weather.ui.later

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import kotlinx.coroutines.launch
import mii.weather.adapters.LaterTodayAdapter
import mii.weather.databinding.LaterTodayWeatherFragmentBinding
import mii.weather.models.AirPollutionResult
import mii.weather.models.OneCallWeatherResult
import mii.weather.repository.WeatherRepository.Companion.airPollutionResultUpdated
import mii.weather.repository.WeatherRepository.Companion.oneCallWeatherResultUpdated
import mii.weather.ui.CommonViewModel

class LaterTodayWeatherFragment : Fragment() {

    private lateinit var oneCallWeatherResult: OneCallWeatherResult
    private lateinit var airPollutionResult: AirPollutionResult
    private var _binding: LaterTodayWeatherFragmentBinding? = null
    private val binding get() = _binding!!

    private val transition: Transition = Slide(Gravity.TOP)

    private lateinit var commonViewModel: CommonViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commonViewModel = ViewModelProvider(this)[CommonViewModel::class.java]
        _binding = LaterTodayWeatherFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDataObserver()
        airPollutionObserver()
    }

    private fun initDataObserver() {
        commonViewModel.oneCallWeatherResult.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                oneCallWeatherResult = it
                if (binding.coverLater.isVisible) {
                    TransitionManager.beginDelayedTransition(binding.root, transition)
                    binding.coverLater.isVisible = false
                }
            }
        }
    }

    private fun airPollutionObserver() {
        commonViewModel.airPollutionResult.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                airPollutionResult = it
                updateLaterTodayRecycler(oneCallWeatherResult, airPollutionResult)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        commonViewModel.getEightWeatherData()
        oneCallWeatherResultUpdated?.let {
            airPollutionResultUpdated?.let { it1 ->
                updateLaterTodayRecycler(
                    it,
                    it1
                )
            }
        }
    }

    private fun updateLaterTodayRecycler(
        oneCallWeatherResult: OneCallWeatherResult,
        airPollutionResult: AirPollutionResult
    ) {
        binding.recyclerLaterToday.adapter =
            LaterTodayAdapter(oneCallWeatherResult, airPollutionResult)
        binding.recyclerLaterToday.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}