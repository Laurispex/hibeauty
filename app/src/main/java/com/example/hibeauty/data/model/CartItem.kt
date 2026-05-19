package com.example.hibeauty.data.model

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val presentation: String = "",
    val price: Long = 0L,
    val quantity: Long = 1L
)
