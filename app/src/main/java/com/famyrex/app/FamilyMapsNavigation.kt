package com.famyrex.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun openFamilyLocationInMaps(context: Context, latitude: Double, longitude: Double, label: String? = null) {
    val mapsQuery = if (label.isNullOrBlank()) {
        "$latitude,$longitude"
    } else {
        Uri.encode("$latitude,$longitude($label)")
    }
    val googleMapsUri = Uri.parse("geo:$latitude,$longitude?q=$mapsQuery")
    val intent = Intent(Intent.ACTION_VIEW, googleMapsUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("$latitude,$longitude")}")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

fun navigateFamilyLocationInMaps(context: Context, latitude: Double, longitude: Double) {
    val navigationIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("google.navigation:q=$latitude,$longitude")
    ).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(navigationIntent)
    } catch (_: ActivityNotFoundException) {
        val webUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode("$latitude,$longitude")}" +
                "&travelmode=driving"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
