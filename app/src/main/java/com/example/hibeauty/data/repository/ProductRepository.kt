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
        getAllProducts().filter { it.isActive }
    }

    suspend fun getProductsByCategory(category: String): Result<List<Product>> = runCatching {
        getAllProducts().filter {
            it.isActive && it.category.equals(category, ignoreCase = true)
        }
    }

    suspend fun getProductById(productId: String): Result<Product> = runCatching {
        collection.document(productId).get().await().toProduct()
    }

    suspend fun getAllForStore(): Result<List<Product>> = runCatching {
        getAllProducts()
    }

    // ─── WRITE ─────────────────────────────────────────────────────────────────

    suspend fun saveProduct(product: Map<String, Any>, productId: String? = null): Result<String> =
        runCatching {
            if (productId != null) {
                val withId = product.toMutableMap().also { it["id"] = productId }
                collection.document(productId).set(withId).await()
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

    private suspend fun getAllProducts(): List<Product> {
        return collection
            .get()
            .await()
            .documents
            .map { it.toProduct() }
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
