package com.example.locationnotes

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.util.*

open class LTActivity : AppCompatActivity() {
    lateinit var dbHelper: DBHelper
    private val permissionId = 44

    var location: Location = defaultLocation
    val latitude get() = location.latitude
    val longitude get() = location.longitude

    private var isLocationReady = false

    lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()
        dbHelper = DBHelper(baseContext)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (intent?.action == null) {
            val shouldClose = onBackButtonClick()
            if (shouldClose) {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onLocationChange(location: Location) {
        this.location = location
        locationDidChange()

        if (!isLocationReady) {
            isLocationReady = true
            onLocationReady()
        }
    }

    open fun locationDidChange() {}
    open fun onLocationReady() {}

    fun checkPermission(): Boolean {
        val permissionsGranted =
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (permissionsGranted) {
            return true
        }
        return false
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionId) {
            if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                // Granted, get location information
                getLastLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                requestNewLocationData()
            } else {
                Toast.makeText(
                    this,
                    "Turn on location",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LocationManager::class.java)
        val isEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isEnabled
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
        }

        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }

    val mLocationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            onLocationChange(location)
        }
    }

    fun Place.Companion.getById(uid: Long) = Place.getAll().find { it.uid == uid }
    fun Place.Companion.getAll() = retrievePlaceList()

    @SuppressLint("Range")
    private fun retrievePlaceList(): List<Place> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM places", null)

        var placeList = listOf<Place>()
        while (cursor.moveToNext()) {
            val uid = cursor.getLong(cursor.getColumnIndex("uid"))
            val lat = cursor.getDouble(cursor.getColumnIndex("lat"))
            val lon = cursor.getDouble(cursor.getColumnIndex("lon"))
            val time = cursor.getLong(cursor.getColumnIndex("time"))
            val description = cursor.getString(cursor.getColumnIndex("description"))
            val hue = cursor.getInt(cursor.getColumnIndex("hue"))
            placeList += Place(uid, lat, lon, time, description, hue)
        }
        cursor.close()
        return placeList
    }

    fun Place.Companion.addLocation(location: Location) = saveLocation(location)

    private fun saveLocation(location: Location): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("lat", location.latitude)
            put("lon", location.longitude)
            put("time", Date().time)
            put("description", "")
            put("hue", 30)
        }

        val rowId = db.insert("places", null, cv)
        if (rowId > 0) {
            debugNotification("Successfully Inserted, Row Id is $rowId")
        }
        dbHelper.close()
        return rowId
    }

    fun Place.Companion.delete(place: Place) = deletePlace(place)

    private fun deletePlace(place: Place) {
        dbHelper.writableDatabase.let {
            val rowId = it.delete(
                "places",
                "uid=?",
                arrayOf(place.uid.toString())
            )
            if (rowId > 0) {
                debugNotification("Successfully deleted")
            }
        }
    }

    fun Place.Companion.update(place: Place, description: String, hue: Int) =
        updatePlace(place, description, hue)

    private fun updatePlace(place: Place, description: String, hue: Int) {
        dbHelper.writableDatabase.let { db ->
            val cv = ContentValues().apply {
                put("description", description.ifBlank { "" })
                put("hue", hue)
            }
            val rowId = db.update(
                "places",
                cv,
                "uid=?",
                arrayOf<String>(place.uid.toString())
            )
            if (rowId > 0) {
                debugNotification("Successfully Updated")
            }
        }
    }

    protected fun debugNotification(text: String) {
        if (debugMode) {
            Toast.makeText(
                baseContext,
                text,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun showPlaceInfo(place: Place) {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra("uid", place.uid)
        startActivity(intent)
    }

    open fun onBackButtonClick() : Boolean {
        return true
    }

    protected fun Number.toIcon() =
        BitmapDescriptorFactory.defaultMarker(this.toFloat())
}