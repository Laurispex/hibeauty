package com.example.hibeauty.data.model

data class RoutineStep(
    val id: String = "",
    val title: String = "",
    val duration: String = "",
    val isCompleted: Boolean = false,
    val order: Int = 0
)
