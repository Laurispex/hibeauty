package com.example.hibeauty.data.mapper

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.ProductPresentation
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toProduct(): Product {
    val rawPresentations = get("presentations") as? Map<*, *> ?: emptyMap<String, Any>()

    var parsedPresentations = rawPresentations.mapNotNull { entry ->
        val size = entry.key as? String ?: return@mapNotNull null
        val values = entry.value as? Map<*, *> ?: return@mapNotNull null
        val price = (values["price"] as? Number)?.toLong() ?: 0L
        val stock = (values["stock"] as? Number)?.toLong() ?: 0L
        size to ProductPresentation(price = price, stock = stock)
    }.toMap()

    if (parsedPresentations.isEmpty()) {
        val rootPrice = (get("price") as? Number)?.toLong() ?: 0L
        val rootStock = (get("stock") as? Number)?.toLong() ?: 0L
        if (rootPrice > 0 || rootStock > 0) {
            parsedPresentations = mapOf("Única" to ProductPresentation(price = rootPrice, stock = rootStock))
        }
    }

    val status = getString("status")?.lowercase()?.trim()
    val isActive = getBoolean("isActive")
        ?: getBoolean("active")
        ?: (status == null || status !in setOf("inactive", "paused", "pausado", "eliminado"))

    return Product(
        id = getString("id") ?: id,
        name = getString("name") ?: getString("title") ?: getString("nombre") ?: getString("titulo") ?: "Producto sin nombre",
        description = getString("description") ?: getString("desc") ?: getString("descripcion") ?: "",
        imageUrl = getString("imageUrl") ?: getString("image") ?: getString("imagen") ?: "",
        category = getString("category") ?: getString("categoria") ?: "",
        benefits = getString("benefits") ?: getString("beneficios") ?: "",
        howToUse = getString("howToUse") ?: getString("modo_de_uso") ?: "",
        isActive = isActive,
        isFeatured = getBoolean("isFeatured") ?: false,
        isNew = getBoolean("isNew") ?: false,
        isOffer = getBoolean("isOffer") ?: false,
        oldPrice = getLong("oldPrice") ?: (get("oldPrice") as? Number)?.toLong() ?: 0L,
        presentations = parsedPresentations
    )
}
