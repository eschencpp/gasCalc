package com.example.gascalc

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.gascalc.databinding.ActivityMapsBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Marker
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener
import java.util.concurrent.TimeUnit

private const val TAG = "MainActivity"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    lateinit var toggle: ActionBarDrawerToggle
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null


    private var latitude : Double? = null
    private var longitude : Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.locationFab.setOnClickListener{
            getLocation()
            latitude?.let { it1 -> longitude?.let { it2 -> goToLocation(it1, it2) } }
            var latlng : LatLng = LatLng(latitude!!, longitude!!)
            mMap.addMarker(MarkerOptions().position(latlng).title("Your Location"))
            removeLocationUpdateCallbacks()
        }

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

        //Bottom sliding menu
        binding.calcButton.setOnClickListener {
            val dialog : Dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.fragment_bottom)

            dialog.show()
            dialog.getWindow()!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.getAttributes().windowAnimations = R.style.DialogAnimation
            dialog.getWindow()!!.setGravity(Gravity.BOTTOM)
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkPermissions()
        initLocRequest()
        getLocation()
        //latitude?.let { longitude?.let { it1 -> goToLocation(it, it1) } }
    }

    fun initLocRequest(){
        locationRequest = LocationRequest().apply {

            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult?.lastLocation?.let {
                    currentLocation = locationResult.lastLocation
                    latitude = currentLocation?.latitude
                    longitude = currentLocation?.longitude
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


}