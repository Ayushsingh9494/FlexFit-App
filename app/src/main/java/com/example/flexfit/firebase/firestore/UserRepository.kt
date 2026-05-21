package com.example.flexfit.firebase.firestore

import com.example.flexfit.firebase.models.UserProfile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val manager: FirestoreManager = FirestoreManager()
) {

    fun observeProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val registration: ListenerRegistration = manager.profileDocument(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserProfile::class.java))
            }
        awaitClose { registration.remove() }
    }

    suspend fun getProfile(userId: String): UserProfile? {
        return manager.profileDocument(userId).get().await().toObject(UserProfile::class.java)
    }

    suspend fun ensureProfileSeed(profile: UserProfile) {
        val existing = getProfile(profile.uid)
        if (existing == null) {
            manager.profileDocument(profile.uid).set(profile).await()
        }
    }

    suspend fun upsertProfile(profile: UserProfile) {
        manager.profileDocument(profile.uid).set(profile).await()
    }
}
