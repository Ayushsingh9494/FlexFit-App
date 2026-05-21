package com.example.flexfit.firebase.firestore

import com.example.flexfit.firebase.models.ReminderData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReminderRepository(
    private val manager: FirestoreManager = FirestoreManager()
) {

    fun observeReminders(userId: String): Flow<List<ReminderData>> = callbackFlow {
        val registration = manager.remindersCollection(userId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { it.toObject<ReminderData>() }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun upsertReminder(userId: String, reminder: ReminderData) {
        manager.remindersCollection(userId).document(reminder.id).set(reminder).await()
    }

    suspend fun deleteReminder(userId: String, reminderId: String) {
        manager.remindersCollection(userId).document(reminderId).delete().await()
    }
}
