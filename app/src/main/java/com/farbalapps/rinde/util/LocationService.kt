package com.farbalapps.rinde.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Obtiene la ubicación actual del dispositivo.
     * Requiere permisos manifest: ACCESS_FINE_LOCATION o ACCESS_COARSE_LOCATION.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene una dirección legible a partir de coordenadas.
     */
    fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val featureName = address.featureName
                val thoroughfare = address.thoroughfare
                val subLocality = address.subLocality
                val locality = address.locality
                
                val parts = listOfNotNull(featureName ?: thoroughfare, subLocality, locality).distinct()
                parts.joinToString(", ")
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
