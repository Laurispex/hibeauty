package com.example.hibeauty.data.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val address: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val paymentMethod: String = "Contraentrega",
    val subtotal: Long = 0L,
    val shipping: Long = 0L,
    val total: Long = 0L,
    val earnedPoints: Long = 0L,
    val riderId: String = "",
    val riderName: String = "",
    val riderPhone: String = "",
    val items: List<OrderItem> = emptyList(),
    val createdAtMillis: Long = 0L
)

data class OrderItem(
    val productId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val presentation: String = "",
    val quantity: Long = 0L,
    val price: Long = 0L
)
