package com.example.hibeauty.data.repository

import com.example.hibeauty.data.mapper.toProduct
import com.example.hibeauty.data.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collection = db.collection("products")

    // ─── READ ──────────────────────────────────────────────────────────────────

    suspend fun getActiveProducts(): Result<List<Product>> = runCatching {
        // Se descargan todos para que los productos antiguos (sin el campo isActive)
        // se puedan mapear correctamente a true y no sean descartados por Firestore.
        collection
            .get()
            .await()
            .documents
            .map { it.toProduct() }
            .filter { it.isActive }
    }

    suspend fun getProductsByCategory(category: String): Result<List<Product>> = runCatching {
        collection
            .whereEqualTo("category", category)
            .get()
            .await()
            .documents
            .map { it.toProduct() }
            .filter { it.isActive }
    }

    suspend fun getProductById(productId: String): Result<Product> = runCatching {
        collection.document(productId).get().await().toProduct()
    }

    suspend fun getAllForStore(): Result<List<Product>> = runCatching {
        // Obtenemos todos los productos y los ordenamos por fecha de creación (los más nuevos arriba).
        // Los productos antiguos que no tengan fecha quedarán al final.
        collection.get().await().documents
            .map { doc -> doc.toProduct() to (doc.getDate("createdAt")?.time ?: 0L) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    // ─── WRITE ─────────────────────────────────────────────────────────────────

    suspend fun saveProduct(product: Map<String, Any>, productId: String? = null): Result<String> =
        runCatching {
            if (productId != null) {
                collection.document(productId).set(product).await()
                productId
            } else {
                val ref = collection.document()
                val withId = product.toMutableMap().also { it["id"] = ref.id }
                ref.set(withId).await()
                ref.id
            }
        }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        collection.document(productId).delete().await()
    }
}
