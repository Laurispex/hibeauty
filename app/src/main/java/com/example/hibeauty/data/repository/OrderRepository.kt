package com.example.hibeauty.data.repository

import com.example.hibeauty.data.mapper.toOrder
import com.example.hibeauty.data.model.Order
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class OrderRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collection = db.collection("orders")

    // ─── READ ──────────────────────────────────────────────────────────────────

    suspend fun getOrdersByUser(userId: String): Result<List<Order>> = runCatching {
        collection
            .whereEqualTo("userId", userId)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .map { it.toOrder() }
    }

    suspend fun getAllOrders(): Result<List<Order>> = runCatching {
        collection
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .map { it.toOrder() }
    }

    suspend fun getOrdersByStatus(status: String): Result<List<Order>> = runCatching {
        collection
            .whereEqualTo("status", status)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .map { it.toOrder() }
    }

    suspend fun getRecentOrders(limit: Long = 5): Result<List<Order>> = runCatching {
        collection
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .map { it.toOrder() }
    }

    suspend fun getOrdersReadyForDelivery(): Result<List<Order>> = runCatching {
        collection
            .whereEqualTo("status", "Listo")
            .orderBy("createdAtMillis", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .map { it.toOrder() }
    }

    // ─── WRITE ─────────────────────────────────────────────────────────────────

    suspend fun createOrder(
        userId: String,
        orderData: Map<String, Any>,
        cartItemIds: List<String>,
        stockBatch: com.google.firebase.firestore.WriteBatch,
        earnedPoints: Long
    ): Result<String> = runCatching {
        val orderRef = collection.document()
        val withId = orderData.toMutableMap().also { it["id"] = orderRef.id }

        stockBatch.set(orderRef, withId)

        // Update user stats
        stockBatch.set(
            db.collection("users").document(userId),
            mapOf(
                "points" to FieldValue.increment(earnedPoints),
                "orderCount" to FieldValue.increment(1),
                "totalSpent" to FieldValue.increment(orderData["total"] as? Long ?: 0L)
            ),
            SetOptions.merge()
        )

        // Clear cart
        cartItemIds.forEach { itemId ->
            stockBatch.delete(
                db.collection("carts").document(userId).collection("items").document(itemId)
            )
        }

        stockBatch.commit().await()
        orderRef.id
    }

    suspend fun updateStatus(orderId: String, status: String, statusLabel: String): Result<Unit> =
        runCatching {
            val historyEntry = hashMapOf(
                "status" to status,
                "label" to statusLabel,
                "changedAtMillis" to System.currentTimeMillis()
            )
            collection.document(orderId).update(
                mapOf(
                    "status" to status,
                    "statusLabel" to statusLabel,
                    "statusUpdatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(historyEntry)
                )
            ).await()
        }

    suspend fun assignRider(orderId: String, riderId: String, riderName: String): Result<Unit> =
        runCatching {
            collection.document(orderId).update(
                mapOf("riderId" to riderId, "riderName" to riderName)
            ).await()
        }
}
