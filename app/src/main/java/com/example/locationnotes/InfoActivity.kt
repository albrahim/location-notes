package com.example.locationnotes

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import com.example.locationnotes.databinding.ActivityInfoBinding

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class InfoActivity : LTActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityInfoBinding

    var selectedLocationMarker: Marker? = null
    var place: Place? = null

    var selectedHue: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.colors.visibility = View.GONE

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.setAllGesturesEnabled(false)
        mMap.isBuildingsEnabled = false
        mMap.isIndoorEnabled = false

        val uid = intent.getLongExtra("uid", 0)
        Place.getById(uid)?.also { place ->
            this.place = place

            val position = LatLng(place.lat, place.lon)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))
            showPlaceMarker(place)

            binding.descriptionEdit.setText(place.description)
            binding.descriptionEdit.setSelection(place.description.length)

            binding.descriptionEdit.doOnTextChanged { text,_,_,_ ->
                selectedLocationMarker?.let {
                    it.title = place.displayDescriptionForString(text.toString())
                    if (it.isInfoWindowShown) {
                        it.showInfoWindow()
                    }
                }
            }

            binding.closeButton.setOnClickListener {
                commitChanges()
                finish()
            }

            binding.discardButton.setOnClickListener {
                finish()
            }

            binding.deleteButton.setOnClickListener {
                Place.delete(place)
                finish()
            }

            listOf(
                Pair(binding.color1, 30),
                Pair(binding.color2, 45),
                Pair(binding.color3, 90),
                Pair(binding.color4, 200),
                Pair(binding.color5, 309)
            ).forEach { it ->
                val (button, hue) = it
                button.setOnClickListener {
                    debugNotification(hue.toString())
                    selectedLocationMarker?.let {
                        it.setIcon(hue.toIcon())
                        selectedHue = hue
                    }
                }
            }

            mMap.setOnInfoWindowClickListener { marker ->
                marker.place?.let {
                    MainActivity.placeToSelectOnResume = it
                    finish()
                }
            }

            mMap.setOnMapClickListener {
                binding.colors.visibility = View.GONE
            }

            mMap.setOnMarkerClickListener {
                if (binding.colors.visibility == View.VISIBLE) {
                    binding.colors.visibility = View.GONE
                } else {
                    binding.colors.visibility = View.VISIBLE
                }
                false
            }
        }

    }

    fun showPlaceMarker(place: Place): Marker? {
        val position = LatLng(place.lat, place.lon)
        return mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(place.displayDescription)
                .icon(place.hue.toIcon())
        )?.also {
            it.tag = place
            selectedLocationMarker = it
            it.showInfoWindow()
        }
    }

    override fun onBackButtonClick(): Boolean {
        commitChanges()
        return super.onBackButtonClick()
    }

    private fun commitChanges() {
        place?.let { place ->
            val description = binding.descriptionEdit.text.toString()
            val hue = selectedHue ?: place.hue
            Place.update(place, description, hue)
        }
    }
}
