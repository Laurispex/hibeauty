package com.example.hibeauty.data.repository

import com.example.hibeauty.data.mapper.toUser
import com.example.hibeauty.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentFirebaseUser get() = auth.currentUser

    // ─── AUTH ──────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("UID not found after login")
    }

    suspend fun register(email: String, password: String): Result<String> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("UID not found after register")
    }

    fun logout() = auth.signOut()

    // ─── PROFILE ───────────────────────────────────────────────────────────────

    suspend fun getUserProfile(uid: String): Result<User> = runCatching {
        db.collection("users").document(uid).get().await().toUser(uid)
    }

    suspend fun createUserProfile(uid: String, data: Map<String, Any>): Result<Unit> =
        runCatching {
            db.collection("users").document(uid).set(data).await()
        }

    suspend fun updateUserProfile(uid: String, data: Map<String, Any>): Result<Unit> =
        runCatching {
            db.collection("users").document(uid).update(data).await()
        }

    suspend fun addEarnings(uid: String, amount: Long): Result<Unit> = runCatching {
        db.collection("users").document(uid).update(
            mapOf(
                "completedDeliveries" to FieldValue.increment(1),
                "earnings" to FieldValue.increment(amount)
            )
        ).await()
    }
}
