package com.uteke.locationforegroundapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationForegroundService : Service() {
    private val binder = Binder()

    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        setUp()
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let {
            when (it.action) {
                Action.START.name -> start()
                Action.STOP.name -> stop()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun setUp() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let(::onNewLocation)
            }
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_IN_MILLIS)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_IN_MILLIS)
            .build()
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location sender channel",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableLights(true)
                lightColor = Color.RED
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentTitle("Location sender")
                .setContentText("Sending your location")
                .setColor(Color.BLACK)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(this)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
                .setContentTitle("Location sender")
                .setContentText("Sending your location")
                .setColor(Color.BLACK)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }

        builder.build()
            .apply {
                contentIntent = PendingIntent.getActivity(
                    this@LocationForegroundService,
                    PENDING_INTENT_REQUEST_CODE,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )
            }
            .also { notification ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
    }

    private fun onNewLocation(location: Location) {
        Log.d("Location", "Sending my new location is [${location.latitude};${location.longitude}]")

        Toast
            .makeText(
                this,
                "Sending my new location is [${location.latitude};${location.longitude}]",
                Toast.LENGTH_SHORT,
            )
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun start() {
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper(),
        )
    }

    private fun stop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    enum class Action { START, STOP }

    private companion object {
        const val INTERVAL_IN_MILLIS = 30_000L
        const val FASTEST_INTERVAL_IN_MILLIS = 15_000L
        const val NOTIFICATION_ID = 654321
        const val NOTIFICATION_CHANNEL_ID = "location_sender.channel"
        const val PENDING_INTENT_REQUEST_CODE = 12345
    }
}