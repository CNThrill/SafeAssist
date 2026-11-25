package com.safeassist.maramag.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.safeassist.maramag.model.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrLastKnownLocation(timeoutMs: Long = 6000L): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.Unavailable("Location permission needed. Using my saved address.")
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationResult.Unavailable("Location service unavailable. Using my saved address.")

        if (!isLocationEnabled()) {
            return LocationResult.Unavailable("Turn on device Location to detect GPS.")
        }

        val lastKnown = getBestLastKnownLocation(lm)
        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time <= RECENT_LOCATION_MAX_AGE_MS) {
            return lastKnown.toAvailableResult()
        }

        // Race all enabled providers (Fused, GPS, Network) so both offline GPS and indoor fixes work
        val freshLocation = withTimeoutOrNull(timeoutMs) {
            requestBestAvailableLocation(lm)
        }
        if (freshLocation != null) {
            return freshLocation.toAvailableResult()
        }

        return lastKnown?.toAvailableResult()
            ?: LocationResult.Unavailable("Could not detect GPS signal. Using my saved address.")
    }

    private fun Location.toAvailableResult(): LocationResult.Available = LocationResult.Available(
        latitude = latitude,
        longitude = longitude
    )

    @SuppressLint("MissingPermission")
    private suspend fun requestBestAvailableLocation(lm: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val providers = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    lm.hasProvider(LocationManager.FUSED_PROVIDER) &&
                    lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)
                ) {
                    add(LocationManager.FUSED_PROVIDER)
                }
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    add(LocationManager.GPS_PROVIDER)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    add(LocationManager.NETWORK_PROVIDER)
                }
            }

            if (providers.isEmpty()) {
                if (cont.isActive) cont.resume(null)
                return@suspendCancellableCoroutine
            }

            val lock = Any()
            val completed = AtomicBoolean(false)
            val remaining = AtomicInteger(providers.size)
            val cancellationSignals = mutableListOf<CancellationSignal>()
            val legacyListeners = mutableListOf<LocationListener>()

            fun cleanup() {
                val signalsCopy: List<CancellationSignal>
                val listenersCopy: List<LocationListener>
                synchronized(lock) {
                    signalsCopy = cancellationSignals.toList()
                    listenersCopy = legacyListeners.toList()
                }
                signalsCopy.forEach { it.cancel() }
                listenersCopy.forEach { listener ->
                    try {
                        lm.removeUpdates(listener)
                    } catch (_: Exception) {}
                }
            }

            fun finishWith(location: Location?) {
                if (location != null) {
                    if (completed.compareAndSet(false, true)) {
                        cleanup()
                        if (cont.isActive) cont.resume(location)
                    }
                } else if (remaining.decrementAndGet() <= 0 && completed.compareAndSet(false, true)) {
                    cleanup()
                    if (cont.isActive) cont.resume(null)
                }
            }

            cont.invokeOnCancellation {
                completed.set(true)
                cleanup()
            }

            for (provider in providers) {
                if (completed.get()) break
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    synchronized(lock) {
                        cancellationSignals.add(signal)
                    }
                    try {
                        lm.getCurrentLocation(
                            provider,
                            signal,
                            ContextCompat.getMainExecutor(context)
                        ) { location ->
                            finishWith(location)
                        }
                    } catch (_: Exception) {
                        finishWith(null)
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            finishWith(location)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {
                            finishWith(null)
                        }
                    }
                    synchronized(lock) {
                        legacyListeners.add(listener)
                    }
                    try {
                        @Suppress("DEPRECATION")
                        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    } catch (_: Exception) {
                        finishWith(null)
                    }
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(lm: LocationManager): Location? {
        val now = System.currentTimeMillis()
        val providers = lm.getProviders(true)
        var best: Location? = null
        for (p in providers) {
            val loc = try {
                lm.getLastKnownLocation(p)
            } catch (_: Exception) {
                null
            } ?: continue
            if (now - loc.time > LAST_KNOWN_MAX_AGE_MS) continue
            if (best == null || loc.accuracy < best.accuracy) {
                best = loc
            }
        }
        return best
    }

    companion object {
        private const val RECENT_LOCATION_MAX_AGE_MS = 30_000L
        private const val LAST_KNOWN_MAX_AGE_MS = 15 * 60_000L
    }
}
