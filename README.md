# mii-weather
<h3>Weather App (Android / Kotlin)</h3>
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/icmii.png" width="88">
<p><b>Mii Weather</b> is the weather app that provides <b>current</b>, <b>hourly</b> and <b>8 days</b> forecast,<br>that is available by query search for any place on Earth.<br><br>
<a href="https://play.google.com/store/apps/details?id=mii.weather">Available on Google Play</a><br><br>
If user gives location permission, the app instantly tracks a device location and provides <b>current local weather</b> information.<br>User can also see the map moving from a default to the current location view.
The app also shows weather map layers that available by click on the weather icon.<br>
All data in the app is fetched from <a href="https://www.openweathermap.org/api">OpenWeathermap</a> API.<br><br>
The app is written in <b>Kotlin</b> from scratch and implements the following:<br><br>
1. App design pattern - <b>MVVM</b><br>
2. <b>ViewPager</b> for displaying three fragments. Two of them - hourly and 8 days fragment - render UI using their own <b>recycler views</b>.<br> 
3. HTTP calls - <b>Retrofit2</b>.<br> 
4. <b>Room database</b> for data handling while using the app.<br>
5. Json parsing - <b>Moshi</b> library.<br> 
6. <b>Picasso</b> library for downloading and caching images.<br>
7. Displaying a location on the map - <b>Google maps SDK</b>.<br>
8. <b>Google analytics - Firebase</b> for analyzing users interaction with the app.</p><br>
<p><b>App Flow:</b><br><br>
When the <b>app starts</b>, it asks the user to grant the location permission.<br><br>
If <b>permission is granted</b>, the app makes an <b>API call</b> with latitude and longitude of device location as query parameters and starts to <b>track location changes</b> instantly.<br><br>
The app makes a <b>new API call</b> and <b>re-renders UI</b> with new data if the device has been moved for more than a defined radius from the location of the previous ‘current location’ request or within defined time frame.<br><br>
If the app has no location permission granted, or network is not available, the app gets the last saved data from the Room database and renders the UI.<br><br>
If there is no data saved in the database, the app does nothing and is waiting for user query.<br><br> 
In case a network or location is not available or turned off, the app shows <b>relevant toast notification</b> to a user.<br><br> 
User can <b>search location by name</b> in any language and press search button and the app will display the weather information for that location, sending an API call via Retrofit2.<br><br>
In case of success, app logic fetching latitude and longitude parameters from response and makes additional query by lat lon. After the response the app has two types of weather data in json format that is saved in local variables. <br><br>
* two queries are made because the necessary API for app proper functionality (the second one) is free of charge and does not provide data by querying text, only by querying (lat, lon). Therefore the first query is made by text from user input to ensure availability of weather data and get its exact location for the additional query by (lat, lon). <br><br>
After that user input in lower case within the place name from api response, concatenated together and separated by @ uses as id for saving weather data in room database with additional parameter date in unix time stamp of the next hour from weather json.<br><br>
It is made not only for data handling reasons throughout the app, but also to prevent redundant api calls.<br><br>
Within user input, the app checks <b>if requested weather data is already available</b> in local database and is valid within one hour bounds from the last saved response (while iterating throught database ,the old data that does not fit validation criteria is deleted). <br><br>
If the data in the DB exists, <b>UI is rendered</b> using the information from the DB. <b>Otherwise</b>, it makes <b>a new API call</b>.<br><br>
Developer: Melamed Maksim<br>
Contact <a href="melamed.maks@gmail.com">e-mail</a></p><br><br></p>
<p float="left" align="center">
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.05.59%20PM.png" alt="mii weather" width="250">
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.07.11%20PM.png" alt="mii weather" width="250">
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.28.58%20PM.png" alt="mii weather" width="250">
</p>
<p float="left" align="center">
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.09.33%20PM.png" alt="mii weather" width="250"> 
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.10.12%20PM.png" alt="mii weather" width="250"> 
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.10.34%20PM.png" alt="mii weather" width="250"> 
</p>
<p float="left" align="center">
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.10.59%20PM.png" alt="mii weather" width="250"> 
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.11.17%20PM.png" alt="mii weather" width="250"> 
<img src="https://github.com/melamedMaks/mii-weather/blob/main/images/Screen%20Shot%202022-03-09%20at%207.17.57%20PM.png" alt="mii weather" width="250">
</p>

