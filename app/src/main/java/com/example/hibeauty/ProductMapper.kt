package com.example.hibeauty

import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toProduct(): Product {
    val rawPresentations = get("presentations") as? Map<*, *> ?: emptyMap<String, Any>()

    val presentations = rawPresentations.mapNotNull { entry ->
        val size = entry.key as? String ?: return@mapNotNull null
        val values = entry.value as? Map<*, *> ?: return@mapNotNull null

        val price = (values["price"] as? Number)?.toLong() ?: 0L
        val stock = (values["stock"] as? Number)?.toLong() ?: 0L

        size to ProductPresentation(
            price = price,
            stock = stock
        )
    }.toMap()

    return Product(
        id = getString("id") ?: id,
        name = getString("name") ?: "",
        description = getString("description") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        category = getString("category") ?: "",
        benefits = getString("benefits") ?: "",
        howToUse = getString("howToUse") ?: "",
        isActive = getBoolean("isActive") ?: true,
        presentations = presentations
    )
}
