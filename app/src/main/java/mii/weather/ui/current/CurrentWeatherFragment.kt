package mii.weather.ui.current

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color

import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import mii.weather.R
import mii.weather.databinding.CurrentWeatherFragmentBinding
import mii.weather.ui.ActivityBackPressedCallback
import mii.weather.ui.CommonViewModel
import mii.weather.models.*

class CurrentWeatherFragment : Fragment(), ActivityBackPressedCallback {

    private var _binding: CurrentWeatherFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var commonViewModel: CommonViewModel
    private val transitionTop: Transition = Slide(Gravity.TOP)
    private val transitionBottom: Transition = Slide(Gravity.BOTTOM)

    //location vars
    private var isLocationPermissionAsked: Boolean = false
    private lateinit var map: GoogleMap
    private val callback = locationCallback()
    private val mapCallback = onMapReadyCallback()
    private lateinit var locationClient: FusedLocationProviderClient
    private val fine = Manifest.permission.ACCESS_FINE_LOCATION
    private val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val launcher = activityResultLauncher()

    private lateinit var lat: String
    private lateinit var lon: String
    private val zoomIn = 12f
    private val zoomOut = 0f
    private var current = false

    private var overLayersOn = false
    private lateinit var precipitationOverlay: TileOverlay
    private lateinit var windOverlay: TileOverlay
    private lateinit var cloudsOverlay: TileOverlay
    private lateinit var tempOverlay: TileOverlay

    private var fullscreen = false

    private var isUiCurrentRendered = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commonViewModel = ViewModelProvider(this)[CommonViewModel::class.java]
        _binding = CurrentWeatherFragmentBinding.inflate(inflater, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(mapCallback)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isLocationPermissionAsked){
            locationClient()
        }
        commonViewModel.getWeatherLastSavedInDB(requireContext())
        setListeners()
        initObservers()
    }

    private fun setListeners() {
        onInternetConnectionListener(requireContext())
        editTextListener()
        searchIconListener()
        localWeatherButtonListener()
        onLayoutChangeListener(view)
        onFullScreenButtonListener()
        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        TransitionManager.beginDelayedTransition(binding.root, transitionBottom)
    }

    private fun initObservers() {
        weatherObserver()
        oneCallWeatherObserver()
        onErrorObserver()
    }

    private fun oneCallWeatherObserver() {
        commonViewModel.oneCallWeatherResult.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                updateCurrentWeather(it)
                overLayersOn = false
                binding.buttonGetLocalWeatherCurrent.isClickable = true
            }
        }
    }

    private fun weatherObserver() {
        commonViewModel.weatherResult.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                updateCurrentData(it)
                resetOverLayers()
                if (binding.coverCurrent.isVisible) {
                    isUiCurrentRendered = true
                }
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun onErrorObserver() {
        commonViewModel.errorHandler.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = View.GONE
            binding.buttonGetLocalWeatherCurrent.isClickable = true
        }
    }

    private fun localWeatherButtonListener() {
        binding.buttonGetLocalWeatherCurrent.setOnClickListener {
            if (::lat.isInitialized) {
                binding.progressBar.visibility = View.VISIBLE
                current = true
                commonViewModel.getWeatherByLatLon(lat, lon, requireContext())
                binding.buttonGetLocalWeatherCurrent.isClickable = false
            } else if (!havePermission()) {
                launcher.launch(arrayOf(fine, coarse))
                errorToast(requireContext(), warningCheckLocationPermission)
            } else {
                errorToast(requireContext(), warningLocation)
                onClickAnimate(it)
                map.uiSettings.setAllGesturesEnabled(false)
                binding.constraintLayoutTop.isVisible = true
            }
            onClickAnimate(it)
            map.uiSettings.setAllGesturesEnabled(false)
        }
    }

    private fun onFullScreenButtonListener() {
        binding.imageViewFullscreenButton.setOnClickListener {
            binding.map.isVisible = false
            when {
                fullscreen -> {
                    fullscreenClose()
                }
                !fullscreen -> {
                    fullscreenOpen()
                }
            }
            onClickAnimate(it)
        }
    }

    private fun editTextListener() {
        binding.editTextCityInputCurrent.setOnEditorActionListener { _, _, _ ->
            getWeatherWithUserInput()
        }
    }

    private fun searchIconListener() {
        binding.imageViewSearchIconCurrent.setOnClickListener {
            getWeatherWithUserInput()
            onClickAnimate(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onInternetConnectionListener(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object :
            ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                noNetworkConnection = false
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                noNetworkConnection = true
            }
        })
    }

    private fun onWeatherIconListener(lat: Double, lon: Double) {
        binding.imageViewIconCurrent.setOnClickListener {
            overLayersOn = if (overLayersOn) {
                binding.imageViewPointerCurrent.colorFilter = null
                resetOverLayers()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoomIn))
                map.uiSettings.setAllGesturesEnabled(false)
                false
            } else {
                binding.imageViewPointerCurrent.setColorFilter(Color.rgb(117, 117, 117))
                setOverLayers()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoomOut))
                map.uiSettings.setAllGesturesEnabled(true)
                true
            }
            onClickAnimate(it)
        }
    }

    private fun onLayoutChangeListener(view: View?) {
        view?.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom - 1 && oldBottom > 0) {
                topUiRendering(false)
            } else if (oldBottom in 0 until bottom) {
                topUiRendering(true)
                binding.editTextCityInputCurrent.clearFocus()
            }
        }
    }

    private fun activityResultLauncher() =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            startLocationUpdates()
        }

    @SuppressLint("MissingPermission") //permission check by havePermission() below
    private fun startLocationUpdates() {

        if (havePermission()) {
            locationClient.requestLocationUpdates(
                createLocationsRequests(),
                callback,
                Looper.getMainLooper()
            )
        } else {
            commonViewModel.getWeatherLastSavedInDB(requireContext())
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun locationClient() {
        isLocationPermissionAsked =  true
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        launcher.launch(arrayOf(fine, coarse))
    }

    private fun createLocationsRequests() =
        LocationRequest.create().apply {
            smallestDisplacement = 1000f
            interval = 60_000 //my interval request
            fastestInterval = 60_000 //if have request from other apps -> give me
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    private fun havePermission(): Boolean {
        val fineResult = ContextCompat.checkSelfPermission(requireContext(), fine)
        val coarseResult = ContextCompat.checkSelfPermission(requireContext(), coarse)
        val granted = PackageManager.PERMISSION_GRANTED
        return fineResult == granted || coarseResult == granted
    }

    private fun locationCallback() = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (noNetworkConnection) {
                binding.progressBar.visibility = View.GONE
            }
            for (location in locationResult.locations) {
                lat = location.latitude.toString()
                lon = location.longitude.toString()
                current = true
                commonViewModel.getWeatherByLatLon(lat, lon, requireContext())
            }
        }
    }

    private fun onMapReadyCallback() = OnMapReadyCallback { googleMap ->
        this.map = googleMap
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.setAllGesturesEnabled(false)
        val coord = LatLng(32.0553815, 34.7635663)
        googleMap.addMarker(
            MarkerOptions()
                .position(coord)
                .visible(false)
        )
    }

    @SuppressLint("MissingPermission")
    private fun setLocation(lat: Double, lon: Double) {
        map.clear()
        val position = LatLng(lat, lon)
        if (!current) {
            map.addMarker(
                MarkerOptions().position(position)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_orange_point))
            )
        }
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoomIn))
        map.isTrafficEnabled = true
        if (havePermission()) {
            map.isMyLocationEnabled = true
        }
    }

    private fun onClickAnimate(it: View) {
        it.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.onclick_animation))
        requireView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun hideKeyboard(it: View?) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(it?.windowToken, 0)
        binding.editTextCityInputCurrent.clearFocus()
    }

    private fun getWeatherWithUserInput(): Boolean {
        binding.buttonGetLocalWeatherCurrent.isClickable = false
        current = false
        binding.progressBar.visibility = View.VISIBLE
        val input = binding.editTextCityInputCurrent.text.toString().lowercase()
        if (input != "") {
            commonViewModel.getWeatherByQuery(input, requireContext())
            binding.editTextCityInputCurrent.text.clear()
            hideKeyboard(view)
            return false
        }
        binding.progressBar.visibility = View.GONE
        return true
    }

    private fun updateCurrentWeather(it: OneCallWeatherResult) {

        val lat = it.lat
        val lon = it.lon

        onWeatherIconListener(lat, lon)

        if (::map.isInitialized) {
            setLocation(lat, lon)
        }

        val date = getLocalDateFromTimeStamp(it.current.dt, it.timezoneOffset)
        binding.textViewDateCurrent.text = date

        val icon = it.current.weather[0].icon
        setIconFromUrl(icon, binding.imageViewIconCurrent)

        val description = "${firstCharUp(it.current.weather[0].description)},"
        binding.textViewDescriptionCurrentText.text = description

        binding.textViewSunriseCurrentValue.text = getLocalHourFromTimeStamp(
            it.current.sunrise, it.timezoneOffset
        )

        binding.textViewSunsetCurrentValue.text = getLocalHourFromTimeStamp(
            it.current.sunset, it.timezoneOffset
        )

        val humidity = "${it.current.humidity}%"
        binding.textViewHumidityCurrentValue.text = humidity

        val visibility = "Visibility: ${it.current.visibility / 1000f} km"
        binding.textViewVisibilityCurrentValue.text = visibility

        val wind = "${it.current.windSpeed} m/s"
        binding.textViewWindCurrentValue.text = wind

        val pressure = "${it.current.pressure} hPa"
        binding.textViewPressureCurrentValue.text = pressure

        binding.textViewUvCurrentValue.text = it.hourly[0].uvi.toString()

        val temp = tempToIntAndString(it.hourly[0].temp)
        binding.textViewTempCurrentText.text = temp

        val feelsLike = "Feels like: ${tempToIntAndString(it.hourly[0].temp)}"
        binding.textViewFeelsLikeCurrent.text = feelsLike

        val tempMin = tempToIntAndString(it.daily[0].temp.min)
        binding.textViewMinTempCurrent.text = tempMin

        val tempMax = tempToIntAndString(it.daily[0].temp.max)
        binding.textViewMaxTempCurrent.text = tempMax

        val until = "Valid untill: ${getLocalHourFromTimeStamp(it.hourly[1].dt, it.timezoneOffset)}"
        binding.textViewFeelValue.text = until

        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        binding.coverCurrent.visibility = View.GONE
    }

    private fun updateCurrentData(weatherResult: WeatherResult) {

        var cityCountry = "${weatherResult.city.name}, ${weatherResult.city.country}"
        binding.textViewCityCountryCurrent.text = cityCountry
        val city = "${weatherResult.city.name}, ..."

        cityCountry = city
        binding.editTextCityInputCurrent.hint = cityCountry

        val population = "Soul: ${weatherResult.city.population}"
        if (weatherResult.city.population > 0) {
            binding.textViewPopulation.text = population
        } else {
            binding.textViewPopulation.text = ""
        }
    }

    private fun topUiRendering(visible: Boolean) {
        binding.map.isVisible = false
        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        if (fullscreen) {
            binding.constraintLayoutTop.isVisible = false
        } else {
            binding.constraintLayoutTop.isVisible = visible
        }
        binding.buttonGetLocalWeatherCurrent.isVisible = visible
        binding.imageViewIconCurrent.isVisible = visible
        binding.imageViewPointerCurrent.isVisible = visible
        binding.imageViewFullscreenButton.isVisible = visible
        binding.map.isVisible = visible
        binding.textViewSearchText.isVisible = !visible
        if (!isUiCurrentRendered) {
            binding.coverCurrent.isVisible = visible
        }
    }

    private fun setOverLayers() {
        cloudsOverlay = map.addTileOverlay(
            TileOverlayOptions()
                .tileProvider(commonViewModel.tileProviderClouds)
        )!!
        windOverlay = map.addTileOverlay(
            TileOverlayOptions()
                .tileProvider(commonViewModel.tileProviderWind)
        )!!
        precipitationOverlay = map.addTileOverlay(
            TileOverlayOptions()
                .tileProvider(commonViewModel.tileProviderPrecipitation)
        )!!
    }

    private fun resetOverLayers() {
        if (overLayersOn) {
            cloudsOverlay.remove()
            windOverlay.remove()
            precipitationOverlay.remove()
            map.uiSettings.setAllGesturesEnabled(false)
        }
    }

    private fun fullscreenOpen() {
        binding.imageViewFullscreenButton.imageTintList =
            ColorStateList.valueOf(Color.rgb(117, 117, 117))
        binding.imageViewIconCurrent.setColorFilter(Color.BLACK)
        binding.constraintLayoutTop.isVisible = false
        TransitionManager.beginDelayedTransition(binding.root, transitionBottom)
        binding.map.isVisible = true
        fullscreen = true
    }

    private fun fullscreenClose() {
        binding.imageViewFullscreenButton.imageTintList =
            ColorStateList.valueOf(Color.rgb(206, 206, 206))
        binding.imageViewIconCurrent.colorFilter = null
        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        binding.constraintLayoutTop.isVisible = true
        binding.map.isVisible = true
        fullscreen = false
    }

    override fun handleActivityOnBackPressed(): Boolean {
        if (fullscreen) {
            topUiRendering(true)
            fullscreenClose()
            return true
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        binding.editTextCityInputCurrent.clearFocus()
    }
}
