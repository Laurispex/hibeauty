package com.example.hibeauty.util

import java.text.NumberFormat
import java.util.Locale

private val copFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
    maximumFractionDigits = 0
}

fun Long.toCOP(): String = copFormat.format(this)
fun Int.toCOP(): String = this.toLong().toCOP()
