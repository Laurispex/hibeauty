package com.example.hibeauty.data.repository

import com.example.hibeauty.data.model.CartItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CartRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun cartRef(userId: String) =
        db.collection("carts").document(userId).collection("items")

    suspend fun getCartItems(userId: String): Result<List<CartItem>> = runCatching {
        cartRef(userId).get().await().documents.map { doc ->
            CartItem(
                id = doc.id,
                productId = doc.getString("productId") ?: "",
                name = doc.getString("name") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                presentation = doc.getString("presentation") ?: "",
                price = doc.getLong("price") ?: 0L,
                quantity = doc.getLong("quantity") ?: 1L
            )
        }
    }

    suspend fun removeItem(userId: String, itemId: String): Result<Unit> = runCatching {
        cartRef(userId).document(itemId).delete().await()
    }

    suspend fun addItemToCart(userId: String, data: Map<String, Any>): Result<Unit> = runCatching {
        cartRef(userId).add(data).await()
    }

    suspend fun clearCart(userId: String): Result<Unit> = runCatching {
        val items = cartRef(userId).get().await()
        val batch = db.batch()
        items.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
