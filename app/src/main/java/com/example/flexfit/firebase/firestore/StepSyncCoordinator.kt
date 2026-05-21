package com.example.flexfit.firebase.firestore

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StepSyncCoordinator {

    private const val PREFS_NAME = "fluxfit_step_sync"
    private const val KEY_LAST_SYNC_DATE = "last_sync_date"
    private const val KEY_LAST_SYNC_STEPS = "last_sync_steps"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"
    private const val MIN_BACKGROUND_STEP_DELTA = 25
    private const val MIN_BACKGROUND_SYNC_INTERVAL_MS = 5 * 60 * 1000L

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun syncImmediate(
        context: Context,
        userId: String,
        stepCount: Int,
        repository: StepRepository = StepRepository()
    ): Boolean = sync(context, userId, stepCount, repository, isBackgroundSync = false)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun syncInBackground(
        context: Context,
        userId: String,
        stepCount: Int,
        repository: StepRepository = StepRepository()
    ): Boolean = sync(context, userId, stepCount, repository, isBackgroundSync = true)

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun sync(
        context: Context,
        userId: String,
        stepCount: Int,
        repository: StepRepository,
        isBackgroundSync: Boolean
    ): Boolean {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val lastDate = prefs.getString(KEY_LAST_SYNC_DATE, null)
        val lastSteps = if (lastDate == today) prefs.getInt(KEY_LAST_SYNC_STEPS, -1) else -1
        val lastSyncAt = if (lastDate == today) prefs.getLong(KEY_LAST_SYNC_AT, 0L) else 0L
        val sanitizedSteps = stepCount.coerceAtLeast(0)
        val now = System.currentTimeMillis()

        val shouldSync = if (isBackgroundSync) {
            sanitizedSteps > lastSteps && (
                lastSteps < 0 ||
                    sanitizedSteps - lastSteps >= MIN_BACKGROUND_STEP_DELTA ||
                    now - lastSyncAt >= MIN_BACKGROUND_SYNC_INTERVAL_MS
                )
        } else {
            sanitizedSteps != lastSteps || lastDate != today
        }

        if (!shouldSync) return false

        repository.upsertTodaySteps(
            userId = userId,
            steps = sanitizedSteps,
            caloriesBurned = sanitizedSteps * 0.04,
            distanceKm = sanitizedSteps * 0.0008
        )

        prefs.edit()
            .putString(KEY_LAST_SYNC_DATE, today)
            .putInt(KEY_LAST_SYNC_STEPS, sanitizedSteps)
            .putLong(KEY_LAST_SYNC_AT, now)
            .apply()

        return true
    }
}
