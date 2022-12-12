package com.example.gascalc

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import com.example.gascalc.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


private const val TAG = "MainActivity"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener{
    val API_KEY = "AIzaSyCcuVvf_deMMVz4KI5Td0pW24-rCglJnwo"
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    lateinit var toggle: ActionBarDrawerToggle
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private var startLatitude : Double? = null
    private var startLongitude : Double? = null
    private var destLatitude : Double? = null
    private var destLongitude : Double? = null
    private val client = OkHttpClient()
    private var startAddress : String = "Your Location"
    private var destAddress : String = "Destination"
    private var distancee : Double? = null
    private var gasPrice : String? = null
    private var mpg : String? = null
    private var startMarker: Marker? = null
    private var destMarker : Marker? = null
    //Viewmodel
    private val viewmodel: sharedViewModel by viewModels()
    private val dialogViewmodel: DialogViewModel by viewModels()
    private lateinit var dataStore: DataStore<Preferences>
    private val dialogFrag : DialogFragment = DialogFragment()
    private var polyLine : Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //Disable night theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        dataStore = createDataStore(name = "data")!!
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Update variables to saved state using datastore
        lifecycleScope.launch(){
            if(read("mpg") != null){
                mpg = read("mpg")
            }
            if(read("gas") != null){
                gasPrice = read("gas")
            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        //Set Observers to update MPG/Gas Price if changed
        viewmodel.getMPG().observe(this) { item: String? ->
            mpg = item
            Log.d("observer", mpg!!)
        }
        viewmodel.getGasPrice().observe(this) { item: String? ->
            gasPrice = item
        }

        //Get location button
        binding.locationFab.setOnClickListener{
            if(fusedLocationProviderClient == null){
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                Log.d("FusedNUll", "Fused is null")
            }
            if(locationRequest == null){
                initLocRequest()
                getLocation()
            }else {
                getLocation()
            }
            //Set start address to user location
            lifecycleScope.launch{
                delay(1000L)
                toLocation(startLatitude.toString(),startLongitude.toString(),0)
            }
        }

        //Bottom sliding menu button
        binding.calcButton.setOnClickListener {
            dialogFrag.show(supportFragmentManager,"dialogFrag")
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Automatically get use location on launch
        navMenu()
        checkPermissions()
        initLocRequest()
        getLocation()
    }

    //Create navigation menu on toolbar
    fun navMenu(){
        //Nav Menu
        toggle = ActionBarDrawerToggle(this,binding.drawerLayout, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.miItem1 ->{
                    supportFragmentManager.popBackStack()
                    lifecycleScope.launch(){
                        save("mpg",mpg!!)
                        save("gas",gasPrice!!)
                        Log.d("savedd", read("mpg")!!)
                    }
                    drawerLayout.closeDrawers()
                }

                R.id.miItem2 ->{
                    val x = supportFragmentManager.findFragmentByTag("list")
                    if (x != null) {
                        Log.d(TAG,"Fragment already exists")
                    }

                    if (x == null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.map, SettingsFragment.newInstance(),"list")
                            .addToBackStack("list").commit()
                        supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                        Log.d(TAG,"Fragment not exists")
                    }
                    drawerLayout.closeDrawers()
                }
            }
            true
        }
    }

    //Initialize the location request
    fun initLocRequest(){
        locationRequest = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(5)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        Log.d("InitLoc","Success")
    }

    //Set the users location (latitude, longitude)
    @SuppressLint("MissingPermission")
    private fun getLocation(){
        if(locationRequest == null){
            initLocRequest()
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult?.lastLocation?.let {
                    currentLocation = locationResult.lastLocation
                    startLatitude = currentLocation?.latitude
                    startLongitude = currentLocation?.longitude
                    if(startMarker == null){
                        var latlng : LatLng = LatLng(startLatitude!!, startLongitude!!)
                        startMarker = mMap.addMarker(MarkerOptions().position(latlng))
                    }
                    dialogViewmodel.setStartLatitude(startLatitude!!)
                    dialogViewmodel.setStartLong(startLongitude!!)

                    startLatitude?.let { it1 -> startLongitude?.let { it2 -> goToLocation(it1, it2) } }
                } ?: run {
                    Log.d("Main", "Location information isn't available.")
                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    //Move map camera to user location
    private fun goToLocation(lat:Double, long:Double){
        val latlng :LatLng = LatLng(lat,long)
        val cameraupdate : CameraUpdate = CameraUpdateFactory.newLatLngZoom(latlng,14F)
        mMap.moveCamera(cameraupdate)
    }

    //Check if location permission enabled
    fun checkPermissions(){
        Dexter.withContext(this).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener{
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    var firstRun = viewmodel.getFirstRun().value
                    if(firstRun == true){
                        Toast.makeText(this@MapsActivity, "Location Enabled", Toast.LENGTH_LONG)
                            .show()
                        viewmodel.setFirstRun(false)
                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(this@MapsActivity, "Location Permission Denied", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    Toast.makeText(this@MapsActivity, "Permission Granted", Toast.LENGTH_LONG)
                        .show()
                }

            }).check()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        Toast.makeText(this, "My position",Toast.LENGTH_LONG).show()
        return false
    }

    private fun removeLocationUpdateCallbacks() {
        val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Location Callback removed.")
            } else {
                Log.d(TAG, "Failed to remove Location Callback.")
            }
        }
    }

    //Get API call to find distance
    fun getDistance(startLat: Double,startLong: Double,endLat: Double,endLong: Double,){
        var startlatlong = startLat.toString()+","+startLong.toString()
        val dest = endLat.toString() + "," + endLong.toString()

        val urlMap = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=${startlatlong}" +
                "&destinations=${dest}&units=metric&key=${API_KEY}"
        val request = Request.Builder()
        .url(urlMap)
        .build()
        Log.d("",urlMap)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            //Parse json
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    var json = JSONObject(response.body!!.string())
                    val rows = json["rows"] as JSONArray
                    val elements = rows[0] as JSONObject
                    val distance = elements["elements"] as JSONArray
                    val distanceArray = distance[0] as JSONObject
                    val distanceData = distanceArray["distance"] as JSONObject

                    val dist = distanceData["value"].toString().toDouble()
                    val distMile : Double = (dist/1000)* 0.62137
                    Log.d("Dist", distMile.toString() )
                    //Set distance and gas cost in viewmodel
                    this@MapsActivity.runOnUiThread(java.lang.Runnable {
                        distancee = ((distMile*10).roundToInt().toDouble())/10
                        dialogViewmodel.setDistance(distancee!!)
                        var viewmodelDist : Double = dialogViewmodel.getDistance().value!!

                        try{
                            var cost = ((viewmodelDist/(mpg?.toDouble()!!) * gasPrice?.toDouble()!!)*100).roundToInt().toDouble()/100
                            dialogViewmodel.setGasPrice(cost.toString())
                        }catch (e : Exception){
                            Toast.makeText(this@MapsActivity, "Please set MPG and Gas Price in Settings."
                                , Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
        })

    }
    //Method to geocode LatLng to Location Name
    suspend fun toLocation(lat: String, long: String, flag: Int) {
        var formatString = ""
        val latlng = lat + "," + long
        //API url
        val urlMap = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latlng}" +
                "&key=${API_KEY}"
        val request = Request.Builder()
            .url(urlMap)
            .build()
        Log.d("apilink",urlMap)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            //Parse json
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }

                    var json = JSONObject(response.body!!.string())
                    val results = json["results"] as JSONArray
                    val resultsObj = results[0] as JSONObject
                    formatString = resultsObj["formatted_address"] as String

                    //Set destination address
                    this@MapsActivity.runOnUiThread(java.lang.Runnable {
                        if(flag == 1){
                            destAddress = formatString
                            dialogViewmodel.setDestAddr(destAddress)
                            Toast.makeText(this@MapsActivity, "Destination set to:\n$destAddress"
                                , Toast.LENGTH_SHORT).show()
                            destMarker!!.title = destAddress
                        //Set starting location address
                        }else {
                            startAddress = formatString
                            dialogViewmodel.setStartAddr(startAddress)
                            Toast.makeText(this@MapsActivity, "Start location set to:\n${dialogViewmodel.getStartAddr().value}"
                                , Toast.LENGTH_SHORT).show()
                            if(startMarker != null){
                                startMarker!!.title = startAddress
                            }
                        }
                    })
                    Log.d("OnResponse", formatString )
                }
            }
        })
    }

    //Method to geocode from LocationName to LatLng
    suspend fun toLatLng(address : String, flag : Int){
        val formatAddress = address.replace(" ","%20")
        val urlMap = "https://maps.googleapis.com/maps/api/geocode/json?address=${formatAddress}" +
                "&key=${API_KEY}"
        val request = Request.Builder()
            .url(urlMap)
            .build()
        Log.d("apilink",urlMap)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    var json = JSONObject(response.body!!.string())
                    val results = json["results"] as JSONArray
                    //If the location could not be found make a toast
                    if(results.length() == 0){
                        Log.d("Not found","Not found")
                        this@MapsActivity.runOnUiThread(java.lang.Runnable {
                            Toast.makeText(this@MapsActivity,"Error location not found."
                                , Toast.LENGTH_SHORT).show()
                        })
                        return
                    }
                    val resultObj = results[0] as JSONObject
                    val geometry = resultObj["geometry"] as JSONObject
                    val locationObj = geometry["location"] as JSONObject
                    val lati = locationObj["lat"] as Double
                    val longi = locationObj["lng"] as Double

                    this@MapsActivity.runOnUiThread(java.lang.Runnable {
                        //Flag used to call destination variation of method
                        if(flag == 1) {
                            //Remove previous marker if null
                            if(destMarker != null){
                                destMarker!!.remove()
                            }
                            destLatitude = lati
                            destLongitude = longi
                            dialogViewmodel.setDestLatitude(destLatitude!!)
                            dialogViewmodel.setDestLong(destLongitude!!)
                            //Add marker to the map
                            var latlng : LatLng = LatLng(destLatitude!!, destLongitude!!)
                            destMarker = mMap.addMarker(MarkerOptions().position(latlng))
                            drawPolyLine()
                            Log.d(
                                "LatLng",
                                destLatitude.toString() + ", " + destLongitude.toString()
                            )
                            lifecycleScope.launch{
                                toLocation(destLatitude.toString(),destLongitude.toString(),1)
                            }
                        } else{
                            //Call start location of method
                            if(startMarker != null){
                                startMarker!!.remove()
                            }
                            startLatitude = lati
                            startLongitude = longi
                            dialogViewmodel.setStartLatitude(startLatitude!!)
                            dialogViewmodel.setStartLong(startLongitude!!)
                            var latlng : LatLng = LatLng(startLatitude!!, startLongitude!!)
                            startMarker = mMap.addMarker(MarkerOptions().position(latlng))
                            drawPolyLine()
                            lifecycleScope.launch{
                                toLocation(startLatitude.toString(),startLongitude.toString(),0)
                            }
                        }
                    })
                }
            }
        })
    }

    private fun drawPolyLine(){
        if(polyLine != null){
            polyLine?.remove()
        }
        polyLine = mMap.addPolyline(
            PolylineOptions()
                .clickable(true)
                .add(
                    LatLng(startLatitude!!, startLongitude!!),
                    LatLng(destLatitude!!,destLongitude!!)))
    }


    //Save data in proto datastore
    private suspend fun save(key : String, value:String){
        val dataStoreKey = preferencesKey<String>(key)
        dataStore.edit { settings ->
            settings[dataStoreKey] = value
        }
    }

    //Read data from proto datastore
    private suspend fun read(key : String): String?{
        val dataStoreKey = preferencesKey<String>(key)
        val preferences = dataStore.data.first()
        return preferences[dataStoreKey]
    }

}