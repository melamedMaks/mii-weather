package mii.weather.ui.eight

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
import mii.weather.adapters.EightDayAdapter
import mii.weather.databinding.EightDayWeatherFragmentBinding
import mii.weather.models.OneCallWeatherResult

import mii.weather.ui.CommonViewModel

class EightDayWeatherFragment : Fragment() {

    private var _binding: EightDayWeatherFragmentBinding? = null
    private val binding get() = _binding!!

    private val transitionTop: Transition = Slide(Gravity.TOP)

    private lateinit var commonViewModel: CommonViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commonViewModel = ViewModelProvider(this)[CommonViewModel::class.java]
        _binding = EightDayWeatherFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDataObserver()
    }

    private fun initDataObserver() {
        commonViewModel.oneCallWeatherResult.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                updateEightDayRecycler(it)
                if (binding.coverScreenEight.isVisible) {
                    TransitionManager.beginDelayedTransition(binding.root, transitionTop)
                    binding.coverScreenEight.isVisible = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
            commonViewModel.getEightWeatherData()

    }

    private fun updateEightDayRecycler(it: OneCallWeatherResult) {
        binding.recyclerEightDay.adapter = EightDayAdapter(it)
        binding.recyclerEightDay.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
    }
}