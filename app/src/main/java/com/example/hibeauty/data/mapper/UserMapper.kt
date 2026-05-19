package com.example.hibeauty.data.mapper

import com.example.hibeauty.data.model.User
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toUser(uid: String): User {
    return User(
        uid = uid,
        name = getString("name") ?: "",
        email = getString("email") ?: "",
        phone = getString("phone") ?: "",
        identification = getString("identification") ?: "",
        role = getString("role") ?: "user",
        address = getString("address") ?: "",
        vehicle = getString("vehicle") ?: "",
        plate = getString("plate") ?: "",
        ordersCount = getLong("ordersCount") ?: 0L,
        points = getLong("points") ?: 0L,
        discounts = getLong("discounts") ?: 0L,
        completedDeliveries = getLong("completedDeliveries") ?: 0L,
        earnings = getLong("earnings") ?: 0L,
        createdAt = getLong("createdAt") ?: 0L
    )
}
