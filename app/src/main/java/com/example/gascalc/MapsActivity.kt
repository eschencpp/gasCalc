package com.example.gascalc


import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.gascalc.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import android.location.Geocoder
import android.location.Address
import android.view.inputmethod.EditorInfo
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.delay
import okhttp3.internal.format


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
    private lateinit var dialog : Dialog
    private lateinit var dialog_button : Button
    private lateinit var start_loc_text : EditText
    private lateinit var end_loc_text : EditText
    private val client = OkHttpClient()
    private var startAddress : String = "Start Location"
    private var destAddress : String = ""
    private var templatitude : Double? = null
    private var templongitude : Double? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.locationFab.setOnClickListener{
            getLocation()
            startLatitude?.let { it1 -> startLongitude?.let { it2 -> goToLocation(it1, it2) } }
            var latlng : LatLng = LatLng(startLatitude!!, startLongitude!!)
            mMap.addMarker(MarkerOptions().position(latlng).title("Your Location"))
            removeLocationUpdateCallbacks()

            //Transfer data to fragment
            var bundle : Bundle = Bundle()
            bundle.putDouble("longitude",startLongitude!!)
            bundle.putDouble("longitude",startLatitude!!)
            val bottomFragment : BottomFragment = BottomFragment()
            bottomFragment.arguments = bundle

            lifecycleScope.launch{
                toLocation("40.714224","-73.961452", 0)
                toLatLng("7993 Windchime St")
            }
        }

        //Bottom sliding menu
        binding.calcButton.setOnClickListener {
            setDialog()
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
                }

                R.id.miItem2 ->{ supportFragmentManager.beginTransaction()
                    .replace(R.id.map, SettingsFragment.newInstance()).addToBackStack("Settings").commit()
                    supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                }
            }
            true
        }
    }

    fun setDialog(){
        Log.d("",startAddress)
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fragment_bottom)
        dialog_button = dialog.findViewById(R.id.cal_button)
        start_loc_text = dialog.findViewById(R.id.editTextStartLocation)
        end_loc_text  = dialog.findViewById(R.id.editTextEndLocation)
        start_loc_text.setText(startAddress)
        start_loc_text.setOnFocusChangeListener{ _, hasFocus ->
            if (hasFocus)
                start_loc_text.setText("")
            else
                start_loc_text.setText(startAddress)
        }
        end_loc_text.setOnFocusChangeListener(){_, hasFocus ->
            if (hasFocus)
                end_loc_text.setText("")
            else
                end_loc_text.setText("Destination")
        }
        end_loc_text.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                toLatLng(end_loc_text.text.toString())
                true
            } else {
                false
            }
        }
        dialog_button.setOnClickListener {
            getDistance(startLatitude!!,startLongitude!!,destLatitude!!,destLongitude!!)
        }
        dialog.show()
        dialog.getWindow()!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()!!.getAttributes().windowAnimations = R.style.DialogAnimation
        dialog.getWindow()!!.setGravity(Gravity.BOTTOM)
    }

    //Get users location
    fun initLocRequest(){
        locationRequest = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    //Set the users location (latitude, longitude)
    @SuppressLint("MissingPermission")
    private fun getLocation(){
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult?.lastLocation?.let {
                    currentLocation = locationResult.lastLocation
                    startLatitude = currentLocation?.latitude
                    startLongitude = currentLocation?.longitude
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
                    Toast.makeText(this@MapsActivity, "Location Enabled", Toast.LENGTH_LONG)
                        .show()
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

        // Add a marker in Sydney and move the camera
        //val sydney = LatLng(-34.0, 151.0)
        //mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        //mMap.setOnMarkerClickListener(this);
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
                    val distMile = (dist/1000)* 0.62137
                    Log.d("Dist", distMile.toString() )
                }
            }
        })

    }
    //Method to geocode LatLng to Location Name
    suspend fun toLocation(lat: String, long: String, flag: Int) {
        var formatString = ""
        val latlng = lat + "," + long
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
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    var json = JSONObject(response.body!!.string())
                    val results = json["results"] as JSONArray
                    val resultsObj = results[0] as JSONObject
                    formatString = resultsObj["formatted_address"] as String

                    this@MapsActivity.runOnUiThread(java.lang.Runnable {
                        if(flag == 1){
                            destAddress = formatString
                        }else {
                            startAddress = formatString
                        }
                    })
                    Log.d("OnResponse", formatString )
                }
            }
        })
    }

    //Method to geocode from LocationName to LatLng
    fun toLatLng(address : String){
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
                    val resultObj = results[0] as JSONObject
                    val geometry = resultObj["geometry"] as JSONObject
                    val locationObj = geometry["location"] as JSONObject
                    val lati = locationObj["lat"] as Double
                    val longi = locationObj["lng"] as Double

                    this@MapsActivity.runOnUiThread(java.lang.Runnable {
                        destLatitude = lati
                        destLongitude = longi
                        Log.d("LatLng",destLatitude.toString() +", " + destLongitude.toString())
                    })
                }
            }
        })
    }

}