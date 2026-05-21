package com.example.flexfit.firebase.firestore

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.flexfit.firebase.models.StepData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StepRepository(
    private val manager: FirestoreManager = FirestoreManager()
) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun observeTodaySteps(userId: String, date: LocalDate = LocalDate.now()): Flow<StepData?> = callbackFlow {
        val registration = manager.stepsCollection(userId)
            .document(date.format(DateTimeFormatter.ISO_DATE))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<StepData>())
            }
        awaitClose { registration.remove() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun observeWeeklyHistory(userId: String, endDate: LocalDate = LocalDate.now()): Flow<List<StepData>> = callbackFlow {
        val startDate = endDate.minusDays(6)
        val registration = manager.stepsCollection(userId)
            .whereGreaterThanOrEqualTo("dateKey", startDate.format(DateTimeFormatter.ISO_DATE))
            .whereLessThanOrEqualTo("dateKey", endDate.format(DateTimeFormatter.ISO_DATE))
            .orderBy("dateKey", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { it.toObject<StepData>() }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun upsertTodaySteps(
        userId: String,
        steps: Int,
        caloriesBurned: Double,
        distanceKm: Double,
        date: LocalDate = LocalDate.now()
    ) {
        val dateKey = date.format(DateTimeFormatter.ISO_DATE)
        val payload = StepData(
            id = dateKey,
            dateKey = dateKey,
            dailyStepCount = steps,
            caloriesBurned = caloriesBurned,
            distanceKm = distanceKm,
            timestamp = System.currentTimeMillis()
        )
        manager.stepsCollection(userId).document(dateKey).set(payload).await()
    }
}
