package com.example.locationnotes

import android.content.Intent
import android.os.Bundle
import com.example.locationnotes.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MainActivity : LTActivity() {
    var isCurrentLocationFollowed = false
    lateinit var binding: ActivityMainBinding

    companion object {
        var placeToSelectOnResume: Place? = null
    }

    // maps
    private lateinit var mMap: GoogleMap

    var currentLocationMarker: Marker? = null
    var markerList = listOf<Marker>()
    // end maps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this::onMapReady)
        mapFragment.setMenuVisibility(false)

        // end maps

        binding.viewButton.setOnClickListener {
            followCurrentLocation()
        }

        binding.saveButton.setOnClickListener {
            val uid = Place.addLocation(location)
            Place.getById(uid)?.also {
                resetMarkerForAllPlaces()
                selectPlaceOnMap(it)

//                val place = it
//
//                val intent = Intent(this, MapsActivity::class.java)
//                intent.action = "specific-place"
//                intent.putExtra("uid", place.uid)
//                startActivity(intent)
            }
        }

        binding.listButton.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun selectPlaceOnMap(place: Place) {
        markerList.find { it.place == place }?.let {
            it.showInfoWindow()
            mapSmoothMove(place.lat, place.lon)
        }
    }

    override fun onResume() {
        super.onResume()
        // if activity setup is finished this is not null
        currentLocationMarker?.let {
            resetMarkerForAllPlaces()

            placeToSelectOnResume?.let {
                placeToSelectOnResume = null
                selectPlaceOnMap(it)
            }
        }
    }

    private fun followCurrentLocation() {
        mapSmoothMove(latitude, longitude) {
            isCurrentLocationFollowed = true
        }
    }

    override fun locationDidChange() {
        currentLocationMarker?.also {
            it.position = LatLng(latitude, longitude)
            if (isCurrentLocationFollowed) {
                mapSmoothMove(latitude, longitude) {
                    followCurrentLocation()
                }
            }
        }
    }

    // map functions
    fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true

        mMap.setOnCameraMoveStartedListener {
            isCurrentLocationFollowed = false
        }
        mMap.setOnInfoWindowClickListener { marker ->
            if (marker == currentLocationMarker) {
                saveCurrentLocation()
                Place.getAll().lastOrNull()?.let { place ->
                    selectPlaceOnMap(place)
                }
            } else {
                marker.place?.let {
                    showPlaceInfo(it)
                }
            }
            marker.place?.let {

            }
        }

        showAllPlaces()
    }

    private fun showAllPlaces() {
        val mapPosition = LatLng(latitude, longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapPosition, 17f))
        currentLocationMarker = mMap.addMarker(
            MarkerOptions()
                .position(mapPosition)
                .title("Your location")
                .icon(
                    BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                ).zIndex(1f)
        )

        resetMarkerForAllPlaces()

        mMap.setOnMarkerClickListener { marker ->
            if (marker == currentLocationMarker) {
                followCurrentLocation()
            } else {
                marker.place?.let { place ->
                    isCurrentLocationFollowed = false
                    mapSmoothMove(place.lat, place.lon)
                    debugNotification(place.toString())
                }
            }
            false
        }
    }

    fun addPlaceMarker(place: Place): Marker? {
        val position = LatLng(place.lat, place.lon)
        val hue = place.hue
        return mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(place.displayDescription)
                .snippet(formattedTimestamp(place.time))
                .icon(hue.toIcon())
                .zIndex(2f)
        )?.also {
            it.tag = place
            markerList += it
        }
    }

    override fun onLocationReady() {
        LatLng(location.latitude, location.longitude).let {
            mMap.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition
                        .builder()
                        .zoom(19f)
                        .bearing(70f)
                        .tilt(67f)
                        .target(it)
                        .build()
                )
            )
        }
        mapSmoothMove(latitude, longitude) {
            followCurrentLocation()
        }
    }

    fun mapSmoothMove(
        latitude: Double,
        longitude: Double,
        callback: () -> Unit = {}
    ) {
        CameraPosition.builder()
            .target(LatLng(latitude, longitude))
            .zoom(17f)
            .bearing(70f)
            .tilt(60f)
            .build()
            .let {
                val callback = object : GoogleMap.CancelableCallback {
                    override fun onCancel() {
                    }

                    override fun onFinish() {
                        callback()
                    }

                }
                mMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(it),
                    callback
                )
            }
    }

    fun saveCurrentLocation() {
        Place.addLocation(location)
        resetMarkerForAllPlaces()
    }

    fun resetMarkerForAllPlaces() {
        val selectedMarkerPlace = markerList.find { it.isInfoWindowShown }?.place
        markerList.forEach { it ->
            it.remove()
        }
        markerList = listOf()
        Place.getAll().forEach {
            addPlaceMarker(it)
        }
        selectedMarkerPlace?.let { selectedPlace ->
            markerList.find { selectedPlace == it.place }?.showInfoWindow()
        }
    }
    // end map functions
}
