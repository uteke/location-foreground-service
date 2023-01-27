package com.uteke.locationforegroundapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class ServiceLocationSender(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
) : LocationSender {
    private val isRunningFlow = MutableStateFlow(isRunning)

    override var isRunning: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_RUNNING, false)
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_IS_RUNNING, value)
            }
        }

    override fun isRunningAsFlow(): Flow<Boolean> = isRunningFlow

    override fun start() {
        if (isRunning.not()) {
            isRunning = true
            isRunningFlow.tryEmit(true)
            startService(LocationForegroundService.Action.START)
        }
    }

    override fun stop() {
        if (isRunning) {
            isRunning = false
            isRunningFlow.tryEmit(false)
            startService(LocationForegroundService.Action.STOP)
        }
    }

    private fun startService(action: LocationForegroundService.Action) {
        Intent(context, LocationForegroundService::class.java)
            .apply {
                this.action = action.name
            }
            .also { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
    }

    private companion object {
        const val KEY_IS_RUNNING = "location_sender.key.is_running"
    }
}