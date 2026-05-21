package com.example.flexfit.firebase.firestore

import com.example.flexfit.firebase.models.ActivityLog
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ActivityRepository(
    private val manager: FirestoreManager = FirestoreManager()
) {

    fun observeRecentActivities(userId: String): Flow<List<ActivityLog>> = callbackFlow {
        val registration = manager.activityLogsCollection(userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { it.toObject<ActivityLog>() }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addActivity(userId: String, activityLog: ActivityLog) {
        manager.activityLogsCollection(userId).document(activityLog.id).set(activityLog).await()
        manager.workoutsCollection(userId).document(activityLog.id).set(activityLog).await()
    }
}
