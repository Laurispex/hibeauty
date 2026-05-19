package com.example.hibeauty

import java.text.NumberFormat
import java.util.Locale

/**
 * Extensión de Kotlin para formatear números como Pesos Colombianos (COP).
 * Ejemplo: 45000 -> "$45.000"
 */
fun Number.toCOP(): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "$${formatter.format(this.toLong())}"
}
