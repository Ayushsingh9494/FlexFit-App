package com.example.flexfit.firebase.firestore

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreManager(
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun userDocument(userId: String): DocumentReference {
        return firestore.collection("users").document(userId)
    }

    fun profileDocument(userId: String): DocumentReference {
        return userDocument(userId).collection("profile").document("core")
    }

    fun stepsCollection(userId: String) = userDocument(userId).collection("steps")

    fun remindersCollection(userId: String) = userDocument(userId).collection("reminders")

    fun workoutsCollection(userId: String) = userDocument(userId).collection("workouts")

    fun activityLogsCollection(userId: String) = userDocument(userId).collection("activity_logs")
}
