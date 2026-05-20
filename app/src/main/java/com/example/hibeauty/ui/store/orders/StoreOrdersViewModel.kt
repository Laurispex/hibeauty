package com.example.hibeauty.ui.store.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StoreOrdersUiState {
    object Loading : StoreOrdersUiState()
    object Empty : StoreOrdersUiState()
    data class Ready(val orders: List<Order>) : StoreOrdersUiState()
    data class Error(val message: String) : StoreOrdersUiState()
}

class StoreOrdersViewModel(
    private val orderRepo: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreOrdersUiState>(StoreOrdersUiState.Loading)
    val uiState: StateFlow<StoreOrdersUiState> = _uiState

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    fun load() {
        viewModelScope.launch {
            _uiState.value = StoreOrdersUiState.Loading
            orderRepo.getAllOrders().fold(
                onSuccess = { orders ->
                    _uiState.value =
                        if (orders.isEmpty()) StoreOrdersUiState.Empty
                        else StoreOrdersUiState.Ready(orders)
                },
                onFailure = { _uiState.value = StoreOrdersUiState.Error("No se pudieron cargar los pedidos") }
            )
        }
    }

    fun advanceStatus(order: Order) {
        val (nextStatus, nextLabel) = nextStatusFor(order.status) ?: run {
            _actionResult.value = "Este pedido ya está finalizado"
            return
        }
        viewModelScope.launch {
            orderRepo.updateStatus(order.id, nextStatus, nextLabel).fold(
                onSuccess = {
                    _actionResult.value = "Pedido actualizado a $nextStatus"
                    load()
                },
                onFailure = { _actionResult.value = "No se pudo actualizar el pedido" }
            )
        }
    }

    private fun nextStatusFor(current: String): Pair<String, String>? = when (current) {
        "Pendiente"       -> "Preparando"    to "Preparando tu compra"
        "Preparando"      -> "Listo"          to "Listo para despacho"
        "Listo"           -> "Domicilio Propio" to "En ruta directa de tienda"
        "Domicilio Propio" -> "Entregado"     to "Pedido entregado"
        else               -> null
    }

    fun clearActionResult() { _actionResult.value = null }
}
