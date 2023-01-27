package com.uteke.locationforegroundapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import android.Manifest.permission.*
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MainActivity : ComponentActivity() {

    private val locationSender: LocationSender by lazy {
        ServiceLocationSender(
            context = this,
            sharedPreferences = getSharedPreferences("location_sender", Context.MODE_PRIVATE)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    checkGooglePlayServices()
                    checkLocationPermissions()

                    val isRunning = locationSender.isRunningAsFlow().collectAsState(initial = false)
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isRunning.value.not()) {
                            Button(modifier = Modifier.wrapContentSize(),
                                onClick = { locationSender.start() },
                                content = {
                                    Text(text = "Start service")
                                }
                            )
                        }

                        if (isRunning.value) {
                            Button(modifier = Modifier.wrapContentSize(),
                                onClick = { locationSender.stop() },
                                content = {
                                    Text(text = "Stop service")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        if (hasLocationPermissions().not()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                456,
            )
        }
    }

    private fun hasLocationPermissions(): Boolean =
        when {
            isPermissionGranted(ACCESS_FINE_LOCATION).not() ||
                    isPermissionGranted(ACCESS_COARSE_LOCATION).not() -> false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    isPermissionGranted(ACCESS_BACKGROUND_LOCATION).not() -> false
            else -> true
        }

    private fun isPermissionGranted(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkGooglePlayServices() {
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS && availability.isUserResolvableError(status)) {
            availability.getErrorDialog(this, status, 123)?.show()
        }
    }
}