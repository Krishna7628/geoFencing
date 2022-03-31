package com.vamsi.geofencing

import android.Manifest
import android.R.attr.radius
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.vamsi.geofencing.databinding.ActivityMapsBinding


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var geofencingClient: GeofencingClient
    private val FINE_LOCATION_ACCESS_REQUEST_CODE = 1001
    private val BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 1002
    private lateinit var locationManager: LocationManager
    private val GEOFENCE_RADIUS: Float = 10F
    private lateinit var geofenceHelper: GeofenceHelper
    private val GEOFENCE_ID = "SOME_GEOFENCE_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

val rlMain = findViewById<RelativeLayout>(R.id.rl_main)
        rlMain.visibility = View.GONE
        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)
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

        addGeoFencingArea(latLng = LatLng(17.475093191440838, 78.38699887088615))
    }

    private fun addGeoFencingArea(latLng: LatLng) {

        if (Build.VERSION.SDK_INT >= 29) {
            //We need background permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                handleMapLongClick(latLng)
                enableUserLocation()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //We show a dialog and ask for permission
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_ACCESS_REQUEST_CODE)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_ACCESS_REQUEST_CODE)
                }
            }

        } else {
            handleMapLongClick(latLng)
            enableUserLocation()
        }
    }

    private fun handleMapLongClick(latLng: LatLng) {
        mMap.clear()
        addMarker(latLng)
        addCircle(latLng)
        addGeofence(latLng)
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(latLng: LatLng){
        val geofence = geofenceHelper.getGeofence(
            GEOFENCE_ID,
            latLng,
            radius.toFloat(),
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()

        geofencingClient.addGeofences(geofencingRequest, pendingIntent!!)
            .addOnSuccessListener {   Toast.makeText(this,"Inside GeoFencing",Toast.LENGTH_SHORT).show()}
            .addOnFailureListener { e ->
                val errorMessage = geofenceHelper.getErrorString(e)
                Toast.makeText(this,errorMessage,Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng)
        mMap.addMarker(markerOptions)
    }

    private fun addCircle(latLng: LatLng) {
        val circleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(GEOFENCE_RADIUS.toDouble())
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0))
        circleOptions.fillColor(Color.argb(64, 255, 0, 0))
        circleOptions.strokeWidth(4F)
        mMap.addCircle(circleOptions)
    }


    private fun enableUserLocation() {
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            val LatLng = getLastBestLocation()
            val currenctLocation = LatLng(LatLng?.latitude!!, LatLng?.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currenctLocation, 16F))
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsArray,
                    FINE_LOCATION_ACCESS_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsArray,
                    FINE_LOCATION_ACCESS_REQUEST_CODE
                )
            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                mMap.setMyLocationEnabled(true);
            } else {
                //We do not have the permission..

            }
        }

        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                //We do not have the permission..
                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastBestLocation(): Location? {
        val locationGPS: Location =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)!!
        val locationNet: Location =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!
        var GPSLocationTime: Long = 0
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime()
        }
        var NetLocationTime: Long = 0
        if (null != locationNet) {
            NetLocationTime = locationNet.getTime()
        }
        return if (0 < GPSLocationTime - NetLocationTime) {
            return locationGPS
        } else {
            return locationNet
        }

    }
}