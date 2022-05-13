package com.example.locationnotes

import android.location.Location
import com.google.android.gms.maps.model.Marker
import java.text.DateFormat
import java.util.*

const val debugMode = false

val defaultLocation = Location("dummyprovider").apply {
    latitude = 25.334626
    longitude = 49.599915
}

val Marker.place get() = this.tag as? Place

val Place.displayDescription get() =
    displayDescriptionForString(this.description)

val Place.blankDescription get() = "Place ${this.uid}"

fun Place.displayDescriptionForString(description: String): String =
    description.lines().firstOrNull{ it.isNotBlank() }?.trim() ?: this.blankDescription

val dateTimeInstance: DateFormat = DateFormat.getDateTimeInstance()
fun formattedTimestamp(timestamp: Long): String =
    dateTimeInstance.format(Date(timestamp))