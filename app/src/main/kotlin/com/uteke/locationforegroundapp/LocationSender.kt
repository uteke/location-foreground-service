package com.uteke.locationforegroundapp

import kotlinx.coroutines.flow.Flow

interface LocationSender {
    val isRunning: Boolean
    fun isRunningAsFlow(): Flow<Boolean>
    fun start()
    fun stop()
}