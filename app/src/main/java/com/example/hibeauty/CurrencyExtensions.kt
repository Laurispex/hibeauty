package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import java.text.NumberFormat
import java.util.Locale

/**
 * Extensión de Kotlin para formatear números como Pesos Colombianos (COP).
 * Ejemplo: 45000 → "$45.000"
 *
 * Este archivo se mantiene en el paquete raíz para compatibilidad con los
 * Fragments que aún no han migrado completamente su import.
 * La implementación canónica vive en util/CurrencyExtensions.kt
 */
fun Number.toCOP(): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "\$${formatter.format(this.toLong())}"
}
