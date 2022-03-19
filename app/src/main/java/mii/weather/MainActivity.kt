package mii.weather

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import mii.weather.databinding.ActivityMainBinding
import mii.weather.models.TAB_TITLES
import mii.weather.ui.ActivityBackPressedCallback
import mii.weather.ui.SectionsPagerAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewPager: ViewPager2
    lateinit var sectionsPagerAdapter: SectionsPagerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sectionsPagerAdapter = SectionsPagerAdapter(this)
        viewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.setText(TAB_TITLES[position])
        }.attach()
    }

    //caching onBackPressed event
    override fun onBackPressed() {
        var isBackHandled = false
        //getting current item index
        val currentItem = viewPager.currentItem
        //retrieving the current fragment that renders in viewPager
        val currentFragment = sectionsPagerAdapter.createFragment(currentItem)

        if (currentFragment is ActivityBackPressedCallback) {
            isBackHandled = currentFragment.handleActivityOnBackPressed()
            println(isBackHandled)
        }

        if (!isBackHandled) {
            //check the index
            if (currentItem > 0)
                //returns previous
                viewPager.currentItem = currentItem - 1
            //else calls super
            else {
                super.onBackPressed()
            }
        }
    }

    /* (Android@Kotlin)
    the method limits FONT(text) SCALE(size) in case the user's preferred ONE is too big to fit UI */
    //override method in MAIN activity:
    override fun attachBaseContext(newBase: Context?) {
        //get the current configurations:
        val currentConfig = Configuration(newBase?.resources?.configuration)
        println(currentConfig.fontScale) // <- erase this line in production
        //copy current to new one for later manipulation:
        val newConfig: Configuration = currentConfig
        //assign UI FONT SCALE limit, (must be float) where 1.0 reflects original ui settings 100%:
        val fontScaleLimit = 1.2f
        //assign SCREEN ZOOM limit (int):
        val densityLimit = 470
        //chek if current FONT SCALE assigned by the user fits UI limitations,
        // AND
        //check if zoom limit assigned by the user fits UI limits:
        if (currentConfig.fontScale > fontScaleLimit && currentConfig.densityDpi > densityLimit) {
            //if not .. ->
            newConfig.fontScale = fontScaleLimit //set your own
            newConfig.densityDpi = densityLimit
            applyOverrideConfiguration(newConfig) //apply new settings
            println(newConfig.fontScale) // <- erase this line in production
        } else if (currentConfig.fontScale > fontScaleLimit) {
            newConfig.densityDpi = densityLimit
            applyOverrideConfiguration(newConfig)
        } else if (currentConfig.densityDpi > densityLimit) {
            newConfig.densityDpi = densityLimit
            applyOverrideConfiguration(newConfig)
        }
        super.attachBaseContext(newBase)
    }
}