package com.example.flexfit.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.flexfit.MainActivity
import com.example.flexfit.R
import com.example.flexfit.firebase.firestore.StepRepository
import com.example.flexfit.firebase.firestore.StepSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class StepCounterService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val stepRepository by lazy { StepRepository() }
    private lateinit var stepCounterManager: StepCounterManager
    private var lastNotificationSteps = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        stepCounterManager = StepCounterManager(applicationContext).apply {
            setOnStepsChanged(::handleStepUpdate)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank() || !stepCounterManager.isSensorAvailable) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat(buildNotification(stepCounterManager.steps.intValue))
        stepCounterManager.startListening()

        serviceScope.launch {
            StepSyncCoordinator.syncInBackground(
                context = applicationContext,
                userId = userId,
                stepCount = stepCounterManager.steps.intValue,
                repository = stepRepository
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stepCounterManager.setOnStepsChanged(null)
        stepCounterManager.stopListening()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleStepUpdate(stepCount: Int) {
        val userId = auth.currentUser?.uid ?: return

        if (lastNotificationSteps < 0 || abs(stepCount - lastNotificationSteps) >= 10) {
            NotificationManagerCompat.from(this).notify(
                NOTIFICATION_ID,
                buildNotification(stepCount)
            )
            lastNotificationSteps = stepCount
        }

        serviceScope.launch {
            StepSyncCoordinator.syncInBackground(
                context = applicationContext,
                userId = userId,
                stepCount = stepCount,
                repository = stepRepository
            )
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(stepCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FluxFit step tracking active")
            .setContentText("Today's steps: $stepCount")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.flexfit.action.START_STEP_COUNTER_SERVICE"

        private const val CHANNEL_ID = "fluxfit_step_tracking"
        private const val CHANNEL_NAME = "FluxFit Step Tracking"
        private const val CHANNEL_DESCRIPTION = "Keeps background step tracking active."
        private const val NOTIFICATION_ID = 1001
    }
}
