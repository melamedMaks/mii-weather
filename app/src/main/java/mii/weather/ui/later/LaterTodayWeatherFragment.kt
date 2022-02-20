package mii.weather.ui.later

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import kotlinx.coroutines.launch

import mii.weather.adapters.LaterTodayAdapter
import mii.weather.databinding.LaterTodayWeatherFragmentBinding
import mii.weather.ui.CommonViewModel
import mii.weather.models.OneCallWeatherResult

class LaterTodayWeatherFragment : Fragment() {

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
    }

    private fun initDataObserver() {
        commonViewModel.oneCallWeatherResult.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                updateLaterTodayRecycler(it)
                if (binding.coverLater.isVisible){
                    TransitionManager.beginDelayedTransition(binding.root, transition)
                    binding.coverLater.isVisible = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
            commonViewModel.getEightWeatherData()
    }

    private fun updateLaterTodayRecycler(it: OneCallWeatherResult) {
        binding.recyclerLaterToday.adapter = LaterTodayAdapter(it)
        binding.recyclerLaterToday.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
    }
}