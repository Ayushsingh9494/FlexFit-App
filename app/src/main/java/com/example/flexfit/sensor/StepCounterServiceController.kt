package com.example.flexfit.sensor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object StepCounterServiceController {

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(appContext, StepCounterService::class.java).apply {
            action = StepCounterService.ACTION_START
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        appContext.stopService(Intent(appContext, StepCounterService::class.java))
    }
}
