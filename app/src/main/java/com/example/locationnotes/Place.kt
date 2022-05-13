package com.example.locationnotes

data class Place(
    val uid: Long,
    val lat: Double,
    val lon: Double,
    val time: Long,
    val description: String,
    val hue: Int
) {
    companion object {}
    override operator fun equals(other: Any?): Boolean {
        val other = other as? Place ?: return false
        return other.uid == this.uid
    }
}
