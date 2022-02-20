package mii.weather.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import mii.weather.ui.current.CurrentWeatherFragment
import mii.weather.ui.eight.EightDayWeatherFragment
import mii.weather.ui.later.LaterTodayWeatherFragment

class SectionsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    private val currentWeatherFragment: CurrentWeatherFragment = CurrentWeatherFragment()
    private val laterTodayWeatherFragment: LaterTodayWeatherFragment = LaterTodayWeatherFragment()
    private val eightDayWeatherFragment: EightDayWeatherFragment = EightDayWeatherFragment()

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> currentWeatherFragment
            1 -> laterTodayWeatherFragment
            2 -> eightDayWeatherFragment
            else ->
                throw IllegalArgumentException("no such fragment")
        }
    }
}

interface ActivityBackPressedCallback {
    fun handleActivityOnBackPressed(): Boolean
}