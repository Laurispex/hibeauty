package com.example.hibeauty

data class Order(
    val id: String = "",
    val userId: String = "",
    val status: String = "",
    val total: Long = 0L,
    val items: List<OrderItem> = emptyList(),
    val riderName: String = "",
    val riderPhone: String = ""
)

data class OrderItem(
    val name: String = "",
    val presentation: String = "",
    val quantity: Long = 0L,
    val price: Long = 0L
)
