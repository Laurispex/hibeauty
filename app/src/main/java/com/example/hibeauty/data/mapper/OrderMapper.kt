package com.example.hibeauty.data.mapper

import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.OrderItem
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toOrder(): Order {
    @Suppress("UNCHECKED_CAST")
    val rawItems = get("items") as? List<Map<String, Any>> ?: emptyList()

    val items = rawItems.map { map ->
        OrderItem(
            productId = map["productId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String ?: "",
            presentation = map["presentation"] as? String ?: "",
            quantity = (map["quantity"] as? Number)?.toLong() ?: 1L,
            price = (map["price"] as? Number)?.toLong() ?: 0L
        )
    }

    return Order(
        id = getString("id") ?: id,
        userId = getString("userId") ?: "",
        userName = getString("userName") ?: "",
        userPhone = getString("userPhone") ?: "",
        address = getString("address") ?: "",
        status = getString("status") ?: "",
        statusLabel = getString("statusLabel") ?: "",
        paymentMethod = getString("paymentMethod") ?: "Contraentrega",
        subtotal = getLong("subtotal") ?: 0L,
        shipping = getLong("shipping") ?: 0L,
        total = getLong("total") ?: 0L,
        earnedPoints = getLong("earnedPoints") ?: 0L,
        riderId = getString("riderId") ?: "",
        riderName = getString("riderName") ?: "",
        riderPhone = getString("riderPhone") ?: "",
        items = items,
        createdAtMillis = getLong("createdAtMillis") ?: 0L
    )
}
