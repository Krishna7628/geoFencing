package com.vamsi.geofencing

import android.Manifest
import android.R.attr.radius
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.google.android.material.snackbar.Snackbar
import com.vamsi.geofencing.databinding.ActivityMapsBinding


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var geofencingClient: GeofencingClient
    private val LOCATION_PERMISSION_INDEX = 0
    private val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private lateinit var locationManager: LocationManager
    private val GEOFENCE_RADIUS: Float = 10F
    private lateinit var geofenceHelper: GeofenceHelper
    private val GEOFENCE_ID = "SOME_GEOFENCE_ID"
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    lateinit var rlMain: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        rlMain = findViewById<RelativeLayout>(R.id.rl_main)
//        rlMain.visibility = View.GONE
        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)
    }


    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            addGeoFencingArea(latLng = LatLng(17.475093191440838, 78.38699887088615))
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkPermissionsAndStartGeofencing()
    }

    private fun addGeoFencingArea(latLng: LatLng) {
        handleMapLongClick(latLng)
        enableUserLocation()

    }

    private fun handleMapLongClick(latLng: LatLng) {
        addMarker(latLng)
        addCircle(latLng)
        addGeofence(latLng)
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(latLng: LatLng) {
        val geofence = geofenceHelper.getGeofence(
            GEOFENCE_ID,
            latLng,
            radius.toFloat(),
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()

        geofencingClient.addGeofences(geofencingRequest, pendingIntent!!)
            .addOnSuccessListener {
                Toast.makeText(this, "Inside GeoFencing", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                val errorMessage = geofenceHelper.getErrorString(e)
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
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


    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        mMap.isMyLocationEnabled = true
        if (isLocationEnabled()) {
            val latLng = getLastBestLocation()
            if(latLng != null) {
                val currenctLocation = LatLng(latLng?.latitude!!, latLng?.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currenctLocation, 19F))
            }else{
                enableUserLocation()
            }
        } else {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }


//    @SuppressLint("MissingPermission")
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
//            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                //We have the permission
//                mMap.setMyLocationEnabled(true);
//            } else {
//                //We do not have the permission..
//
//            }
//        }
//
//        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
//            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                //We have the permission
//                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
//            } else {
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_ACCESS_REQUEST_CODE)
//
//                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                rlMain,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            addGeoFencingArea(latLng = LatLng(17.475093191440838, 78.38699887088615))
        }
    }


    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            this@MapsActivity,
            permissionsArray,
            resultCode
        )
    }


//    @SuppressLint("MissingPermission")
//    private fun getLastBestLocation(): Location? {
//        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        val locationGPS: Location =
//            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)!!
//        val locationNet: Location =
//            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!
//        var GPSLocationTime: Long = 0
//        if (null != locationGPS) {
//            GPSLocationTime = locationGPS.getTime()
//        }
//        var NetLocationTime: Long = 0
//        if (null != locationNet) {
//            NetLocationTime = locationNet.getTime()
//        }
//        return if (0 < GPSLocationTime - NetLocationTime) {
//            return locationGPS
//        } else {
//            return locationNet
//        }
//
//    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLastBestLocation(): Location? {
        var location: Location? = null
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled =
            locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)             // checking for GPS and Network availability
        val isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isGPSEnabled || isNetworkEnabled) {

            var locationGps: Location? = null
            var locationNetwork: Location? = null
            if (isGPSEnabled) {                                                                            // if gps available
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    0f,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (location != null) {
                                locationGps = location
                            }
                        }
                    })

                val lastUpdateLocationFromGPS =
                    locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastUpdateLocationFromGPS != null) {
                    locationGps = lastUpdateLocationFromGPS
                }

                if (isNetworkEnabled) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        10000,
                        0f,
                        object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                if (location != null) {
                                    locationNetwork = location
                                }
                            }

                        })

                    val lastUpdateLocationFromNetwork =
                        locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastUpdateLocationFromNetwork != null) {
                        locationNetwork = lastUpdateLocationFromNetwork
                    }
                }


                if (locationGps != null && locationNetwork != null) {                     // getting accuracy lat and lon
                    if (locationGps!!.accuracy > locationGps!!.accuracy) {
                        return locationNetwork!!
                    } else {
                        return locationGps!!
                    }
                } else {
                    if (locationGps != null) {
                        return locationGps!!
                    } else if (locationNetwork != null) {
                        location
                    }
                }
            }

        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }


        return null
    }

    override fun onResume() {
        super.onResume()
    }
}