package mii.weather.ui.current

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color.rgb
import android.location.Location
import android.location.Location.distanceBetween
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Looper
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mii.weather.R
import mii.weather.databinding.CurrentWeatherFragmentBinding
import mii.weather.models.AirPollutionResult
import mii.weather.models.Alerts
import mii.weather.models.OneCallWeatherResult
import mii.weather.models.WeatherResult
import mii.weather.repository.WeatherRepository.Companion.airPollutionResultUpdated
import mii.weather.repository.WeatherRepository.Companion.oneCallWeatherResultUpdated
import mii.weather.repository.WeatherRepository.Companion.weatherResultUpdated
import mii.weather.ui.ActivityBackPressedCallback
import mii.weather.ui.CommonViewModel
import mii.weather.ui.alerts.AlertsFragment
import mii.weather.ui.alerts.ClickListener
import mii.weather.utils.*

class CurrentWeatherFragment : Fragment(), ActivityBackPressedCallback,
    ClickListener {

    private var _binding: CurrentWeatherFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var commonViewModel: CommonViewModel

    private var onSwipe: HandleOnSwipe? = null

    private val transitionTop: Transition = Slide(Gravity.TOP)
    private val transitionBottom: Transition = Slide(Gravity.BOTTOM)

    //location vars and sets
    private var isLocationPermissionAsked: Boolean = false

    private lateinit var locationClient: FusedLocationProviderClient
    private val fine = Manifest.permission.ACCESS_FINE_LOCATION
    private val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val callback = locationCallback()
    private val launcher = activityResultLauncher()
    private lateinit var lat: String
    private lateinit var lon: String

    //google maps SDK vars and sets
    private lateinit var map: GoogleMap
    private val mapCallback = onMapReadyCallback()
    private val zoomIn: Float = 12f
    private val zoomOut: Float = 0f

    //settings for onLocationChangelistener logic
    private val locationRange: Float = 2000f //smallest displacement in meters 2000f

    //openWeather API weather map layers vars and sets
    private var overLayersOn = false
    private lateinit var precipitationOverlay: TileOverlay
    private lateinit var windOverlay: TileOverlay
    private lateinit var cloudsOverlay: TileOverlay
    //private lateinit var tempOverlay: TileOverlay

    private var fullscreen = false
    private var isUiCurrentRendered = false
    private lateinit var alertsFragment: AlertsFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        commonViewModel = ViewModelProvider(this)[CommonViewModel::class.java]
        _binding = CurrentWeatherFragmentBinding.inflate(inflater, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(mapCallback)
        alertsFragment = AlertsFragment()
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is HandleOnSwipe) {
            onSwipe = activity as HandleOnSwipe
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //to prevent annoying dialog
        if (!isLocationPermissionAsked) {
            locationClient()
        }
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
        onTempTextClickListener()
    }

    private fun initObservers() {
        oneCallWeatherObserver()
        onErrorObserver()
    }

    private fun oneCallWeatherObserver() {
        commonViewModel.oneCallWeatherResult.observe(viewLifecycleOwner) {
            onClickAlertsFragment(isVisible = false)
            lifecycleScope.launch {
                if (it.alerts.isNullOrEmpty()) {
                    hideAlertVisibility()
                } else {
                    binding.textViewAlerts.apply {
                        showAlertView(it.alerts)
                        setOnAlertClickListener()
                        alertVisible = false
                        childFragmentManager.beginTransaction()
                            .replace(R.id.alert_fragment_container, alertsFragment, "AlertFragment")
                            .commit()
                    }
                }
                if (isUIUpdatedByCurrentLocation && isLocationEnabled(requireContext())) {
                    binding.buttonGetLocalWeatherCurrent.imageTintList =
                        ColorStateList.valueOf(rgb(41, 121, 255))
                } else {
                    binding.buttonGetLocalWeatherCurrent.imageTintList =
                        ColorStateList.valueOf(rgb(117, 117, 117))
                }
                binding.progressBar.visibility = View.INVISIBLE
                updateCurrentWeather(it)
                updateCurrentData(weatherResultUpdated)
                airPollutionResultUpdated?.let { it1 -> updateAirPollution(it1) }
                overLayersOn = false
                binding.buttonGetLocalWeatherCurrent.isClickable = true
                resetOverLayers()
                isUiCurrentRendered = true
                inProgress = false
            }
        }
    }

    private fun onErrorObserver() {
        commonViewModel.errorHandler.observe(viewLifecycleOwner) {
            inProgress = false
            binding.progressBar.visibility = View.INVISIBLE
            if (isUIUpdatedByCurrentLocation) {
                binding.buttonGetLocalWeatherCurrent.imageTintList =
                    ColorStateList.valueOf(rgb(41, 121, 255))
            } else {
                binding.buttonGetLocalWeatherCurrent.imageTintList =
                    ColorStateList.valueOf(rgb(117, 117, 117))
            }
            binding.buttonGetLocalWeatherCurrent.isClickable = true
        }
    }

    private fun getWeatherWithUserInput(): Boolean {
        binding.buttonGetLocalWeatherCurrent.isClickable = false
        val input = binding.editTextCityInputCurrent.text.toString().lowercase()
        if (input.isNotEmpty() && !inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonGetLocalWeatherCurrent.imageTintList =
                ColorStateList.valueOf(rgb(255, 255, 255))
            inProgress = true
            commonViewModel.getWeatherByQuery(input, requireContext())
            binding.editTextCityInputCurrent.text.clear()
            hideKeyboard(view)
            return false
        }
        binding.progressBar.visibility = View.INVISIBLE
        return true
    }

    //shows weather map layers by click on the weather condition icon
    private fun localWeatherButtonListener() {
        binding.buttonGetLocalWeatherCurrent.setOnClickListener {
            if (::lat.isInitialized) {
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonGetLocalWeatherCurrent.imageTintList =
                    ColorStateList.valueOf(rgb(255, 255, 255))
                commonViewModel.getWeatherByLatLon(lat, lon, requireContext())
                binding.buttonGetLocalWeatherCurrent.isClickable = false
                latLocal = lat.toDouble()
                lonLocal = lon.toDouble()
            } else if (!havePermission()) {
                launcher.launch(arrayOf(fine, coarse))
                errorToast(requireContext(), warningCheckLocationPermission)
            } else if (isLocationEnabled(requireContext()) && isNetworkAvailable) {
                startLocationUpdates()
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonGetLocalWeatherCurrent.imageTintList =
                    ColorStateList.valueOf(rgb(255, 255, 255))
            } else if (!isNetworkAvailable) {
                errorToast(requireContext(), warningInternet)
                isUIUpdatedByCurrentLocation = false
            } else {
                errorToast(requireContext(), warningLocation)
                isUIUpdatedByCurrentLocation = false
                onClickAnimate(it)
                map.uiSettings.setAllGesturesEnabled(false)
                binding.constraintLayoutTop.isVisible = true
            }
            onClickAnimate(it)
        }
    }

    //shows weather map layers
    private fun onWeatherIconListener(lat: Double, lon: Double) {
        binding.imageViewIconCurrent.setOnClickListener {
            overLayersOn = if (overLayersOn) {
                binding.imageViewPointerCurrent.colorFilter = null
                resetOverLayers()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoomIn))
                false
            } else {
                binding.imageViewPointerCurrent.setColorFilter(rgb(117, 117, 117))
                setOverLayers()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoomOut))
                true
            }
            onClickAnimate(it)
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

    private fun TextView.setOnAlertClickListener() {
        this.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            binding.imageViewPointerAlert.imageTintList =
                ColorStateList.valueOf(rgb(117, 117, 117))
            alertVisible = !alertVisible
            showAlertFragment(isVisible = true)
        }
    }

    private fun onTempTextClickListener() {
        binding.textViewTempUnit.setOnClickListener {
            isFahrenheit = !isFahrenheit
            commonViewModel.getWeatherLastSavedInDB(requireContext())
        }
        binding.textViewTempCurrentText.setOnClickListener {
            isFahrenheit = !isFahrenheit
            commonViewModel.getWeatherLastSavedInDB(requireContext())
        }
    }

    @SuppressLint("MissingPermission")
    private fun onInternetConnectionListener(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object :
            ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isNetworkAvailable = false
            }
        })
    }

    //help func for proper UI rendering
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

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun activityResultLauncher() =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val locationManager =
                context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lifecycleScope.launchWhenResumed {
                while (true) {
                    println("waiting... ${Thread.currentThread()}")
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        startLocationUpdates()
                        println("Counting")
                        return@launchWhenResumed
                    }
                    delay(1000)
                }
            }
        }

    //permission is checked by havePermission() below
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (havePermission()) {
            locationClient.requestLocationUpdates(
                createLocationsRequests(),
                callback,
                Looper.getMainLooper()
            )
            if (!isLocationEnabled(requireContext())) {
                errorToast(requireContext(), warningLocation)
                isUIUpdatedByCurrentLocation = false
            }
        } else {
            commonViewModel.getWeatherLastSavedInDB(requireContext())
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun locationClient() {
        isLocationPermissionAsked = true
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        launcher.launch(arrayOf(fine, coarse))
    }

    private fun createLocationsRequests() =
        LocationRequest.create().apply {
            interval = 120 //once in 2 minutes
            fastestInterval = 0
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
            if (!isNetworkAvailable) {
                binding.progressBar.visibility = View.INVISIBLE
            } else {
                for (location in locationResult.locations) {
                    lat = location.latitude.toString()
                    lon = location.longitude.toString()
                }
                val currentLocation: Location =
                    locationResult.locations[locationResult.locations.size - 1]
                onLocationChangeListener(currentLocation)
            }
        }
    }

    //custom listener that retrieves weather data by current location
    // in case location changed in custom range or period of time
    private fun onLocationChangeListener(location: Location) {
        val result = FloatArray(1)
        distanceBetween(
            latLocal,
            lonLocal, location.latitude, location.longitude, result
        )
        val currentTimeStamp = System.currentTimeMillis()
        if (result[0] > locationRange || (currentTimeStamp - lastSavedTimestamp) > timeRange) {
            println("Before $context")
            if (!inProgress) {
                context?.let {
                    println("After $context")
                    commonViewModel.getWeatherByLatLon(
                        location.latitude.toString(),
                        location.longitude.toString(),
                        it
                    )
                    inProgress = true
                }
            }
            lastSavedTimestamp = currentTimeStamp
            latLocal = location.latitude
            lonLocal = location.longitude
        }
    }

    private fun onMapReadyCallback() = OnMapReadyCallback { googleMap ->
        this.map = googleMap
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.setAllGesturesEnabled(false)
        val coord = LatLng(latLocal, lonLocal)
        googleMap.addMarker(
            MarkerOptions()
                .position(coord)
                .visible(false)
        )
    }

    //permission is checked by havePermission() below
    @SuppressLint("MissingPermission")
    private fun setLocation(lat: Double, lon: Double) {
        map.clear()
        val position = LatLng(lat, lon)
        if (!isUIUpdatedByCurrentLocation) {
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

    private fun hideKeyboard(it: View?) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(it?.windowToken, 0)
        binding.editTextCityInputCurrent.clearFocus()
    }

    //current weather fragment UI data update from oneCall API
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

        val description = firstCharUp(it.current.weather[0].description)
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


        val windDegree = it.daily[0].windDeg + windDegreeCorrection
        binding.currentWindArrow.animate().rotation(windDegree.toFloat()).alpha(1.0f).duration =
            800

        val wind = windFormatter(it.current.windSpeed)
        binding.textViewWindCurrentValue.text = wind

        val pressure = "${it.current.pressure} hPa"
        binding.textViewPressureCurrentValue.text = pressure

        binding.textViewUvCurrentValue.text = it.hourly[0].uvi.toString()

        if (isFahrenheit) {
            binding.textViewTempUnit.text = "F"
        } else {
            binding.textViewTempUnit.text = "C"
        }

        val temp = tempToIntAndString(it.hourly[0].temp)
        binding.textViewTempCurrentText.text = temp

        val feelsLike = "Feels like: ${tempToIntAndString(it.hourly[0].temp)}"
        binding.textViewFeelsLikeCurrent.text = feelsLike

        val tempMin = tempToIntAndString(it.daily[0].temp.min)
        binding.textViewMinTempCurrent.text = tempMin

        val tempMax = tempToIntAndString(it.daily[0].temp.max)
        binding.textViewMaxTempCurrent.text = tempMax

        val until = "Valid until: ${getLocalHourFromTimeStamp(it.hourly[1].dt, it.timezoneOffset)}"
        binding.textViewFeelValue.text = until

        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        binding.coverCurrent.visibility = View.GONE
    }

    //current weather fragment UI data update from currentWeather API
    //used for the reason to make text queries by place name on Earth
    //which aren't exist in oneCal API
    private fun updateCurrentData(weatherResult: WeatherResult) {
        var cityCountry = "${weatherResult.city.name}, ${weatherResult.city.country}"

        binding.textViewCityCountryCurrent.text = cityCountry

        cityCountry = "${weatherResult.city.name}, ${weatherResult.city.country} ..."
        binding.editTextCityInputCurrent.hint = cityCountry

        var population = "Soul: ${weatherResult.city.population / 1000000f} m"
        if (weatherResult.city.population > 0) {
            binding.textViewPopulation.text = population
        } else {
            population = "Soul: no data"
            binding.textViewPopulation.text = population
        }
    }

    private fun updateAirPollution(it: AirPollutionResult) {
        val aqi = aqiCalculation(it.list[0].components)
        var color: Int = rgb(255, 255, 255)
        val text = "AQI: $aqi"
        val pair = getAqiTextColor(aqi, text, color)
        color = pair.first
        binding.airPollutionCurrent.setBackgroundColor(color)
        binding.airPollutionCurrent.text = text
    }

    private fun topUiRendering(visible: Boolean) {
        binding.buttonGetLocalWeatherCurrent.isVisible = visible
        binding.imageViewFullscreenButton.isVisible = visible
        if (alertVisible && binding.alertFragmentContainer.rotationX != 90f) {
            onClickAlertsFragment(alertVisible)
        } else if (alertVisible && binding.alertFragmentContainer.rotationX != 0f) {
            showAlertFragment(alertVisible)
        }
        binding.map.isVisible = false
        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        if (fullscreen) {
            binding.constraintLayoutTop.isVisible = false
        } else {
            binding.constraintLayoutTop.isVisible = visible
        }
        binding.imageViewIconCurrent.isVisible = visible
        binding.imageViewPointerCurrent.isVisible = visible
        binding.map.isVisible = visible
        binding.textViewSearchText.isVisible = !visible
        if (!isUiCurrentRendered) {
            binding.coverCurrent.isVisible = visible
        }
    }

    private fun hideAlertVisibility() {
        binding.textViewAlerts.visibility = View.GONE
        println("No Alerts")
        binding.imageViewPointerAlert.visibility = View.INVISIBLE
    }

    private fun TextView.showAlertView(alerts: List<Alerts>) {
        binding.imageViewPointerAlert.visibility = View.VISIBLE
        this.visibility = View.VISIBLE
        val text = "! ${alerts[0].event}"
        this.text = text
    }

    private fun showAlertFragment(isVisible: Boolean) {
        alertVisible = isVisible
        alertsFragment.setCallback(this)
        binding.textViewAlerts.isClickable = false
        binding.imageViewFullscreenButton.isClickable = false
        binding.imageViewIconCurrent.isClickable = false
        binding.alertFragmentContainer.animate().rotationX(0f)
        println("Alert visible: $alertVisible")
    }

    override fun onClickAlertsFragment(isVisible: Boolean) {
        binding.textViewAlerts.isClickable = true
        binding.imageViewFullscreenButton.isClickable = true
        binding.imageViewIconCurrent.isClickable = true
        binding.textViewAlerts.isClickable = true
        binding.imageViewFullscreenButton.isClickable = true
        binding.imageViewIconCurrent.isClickable = true
        binding.imageViewPointerAlert.imageTintList =
            ColorStateList.valueOf(rgb(206, 206, 206))
        binding.alertFragmentContainer.animate().rotationX(90f)
        alertVisible = isVisible
    }

    //weather map overlays
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
        }
    }

    //UI behavior when in full screen mode
    private fun fullscreenOpen() {
        val lat = oneCallWeatherResultUpdated?.lat
        val lon = oneCallWeatherResultUpdated?.lon
        lat?.let {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lat, lon!!),
                    map.cameraPosition.zoom
                )
            )
        }
        onSwipe?.onSwipe(status = false)
        map.uiSettings.setAllGesturesEnabled(true)
        binding.imageViewFullscreenButton.imageTintList =
            ColorStateList.valueOf(rgb(117, 117, 117))
        binding.constraintLayoutTop.isVisible = false
        TransitionManager.beginDelayedTransition(binding.root, transitionBottom)
        binding.map.isVisible = true
        fullscreen = true
    }

    private fun fullscreenClose() {
        val lat = oneCallWeatherResultUpdated?.lat
        val lon = oneCallWeatherResultUpdated?.lon
        lat?.let {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lat, lon!!),
                    map.cameraPosition.zoom
                )
            )
        }
        onSwipe?.onSwipe(status = true)
        map.uiSettings.setAllGesturesEnabled(false)
        binding.imageViewFullscreenButton.imageTintList =
            ColorStateList.valueOf(rgb(206, 206, 206))
        TransitionManager.beginDelayedTransition(binding.root, transitionTop)
        binding.constraintLayoutTop.isVisible = true
        binding.map.isVisible = true
        fullscreen = false
    }

    //custom event handler for system back button
    override fun handleActivityOnBackPressed(): Boolean {
        if (fullscreen) {
            topUiRendering(true)
            fullscreenClose()
            return true
        }
        return false
    }

    private fun onClickAnimate(it: View) {
        it.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.onclick_animation))
        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun setTempUnit() {
        val sh = activity?.getSharedPreferences("MySharedPref", MODE_PRIVATE)
        when (sh?.getString("isFahrenheit", isFahrenheit.toString())) {
            "true" -> isFahrenheit = true
        }
        commonViewModel.getWeatherLastSavedInDB(requireContext())
    }

    override fun onResume() {
        super.onResume()
        setTempUnit()
    }

    override fun onPause() {
        super.onPause()
        binding.editTextCityInputCurrent.clearFocus()

        val sharedPreferences = activity?.getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val myEdit = sharedPreferences?.edit()
        myEdit?.putString("isFahrenheit", isFahrenheit.toString())
        myEdit?.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface HandleOnSwipe {
    fun onSwipe(status: Boolean)
}
