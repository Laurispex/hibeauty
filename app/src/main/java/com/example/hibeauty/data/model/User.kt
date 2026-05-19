package com.example.hibeauty.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val identification: String = "",
    val role: String = "user",
    val address: String = "",
    val vehicle: String = "",
    val plate: String = "",
    val ordersCount: Long = 0L,
    val points: Long = 0L,
    val discounts: Long = 0L,
    val completedDeliveries: Long = 0L,
    val earnings: Long = 0L,
    val createdAt: Long = 0L
)
