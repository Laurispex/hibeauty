package com.example.hibeauty.data.mapper

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.ProductPresentation
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toProduct(): Product {
    val rawPresentations = get("presentations") as? Map<*, *> ?: emptyMap<String, Any>()

    val presentations = rawPresentations.mapNotNull { entry ->
        val size = entry.key as? String ?: return@mapNotNull null
        val values = entry.value as? Map<*, *> ?: return@mapNotNull null
        val price = (values["price"] as? Number)?.toLong() ?: 0L
        val stock = (values["stock"] as? Number)?.toLong() ?: 0L
        size to ProductPresentation(price = price, stock = stock)
    }.toMap()

    val status = getString("status")?.lowercase()?.trim()
    val isActive = getBoolean("isActive")
        ?: getBoolean("active")
        ?: (status == null || status !in setOf("inactive", "paused", "pausado", "eliminado"))

    return Product(
        id = getString("id") ?: id,
        name = getString("name") ?: "",
        description = getString("description") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        category = getString("category") ?: "",
        benefits = getString("benefits") ?: "",
        howToUse = getString("howToUse") ?: "",
        isActive = isActive,
        isFeatured = getBoolean("isFeatured") ?: false,
        isNew = getBoolean("isNew") ?: false,
        isOffer = getBoolean("isOffer") ?: false,
        oldPrice = getLong("oldPrice") ?: 0L,
        presentations = presentations
    )
}
