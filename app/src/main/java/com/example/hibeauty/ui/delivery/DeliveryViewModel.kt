package com.example.hibeauty.ui.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.repository.OrderRepository
import com.example.hibeauty.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DeliveryUiState {
    object Loading : DeliveryUiState()
    data class Ready(
        val welcomeName: String,
        val completedDeliveries: Long,
        val earnings: Long,
        val availableOrders: List<Order>,
        val activeOrder: Order?
    ) : DeliveryUiState()
    data class Error(val message: String) : DeliveryUiState()
}

class DeliveryViewModel(
    private val orderRepo: OrderRepository = OrderRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeliveryUiState>(DeliveryUiState.Loading)
    val uiState: StateFlow<DeliveryUiState> = _uiState

    fun load() {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = DeliveryUiState.Loading
            val userResult = userRepo.getUserProfile(uid)
            val readyResult = orderRepo.getOrdersReadyForDelivery()
            val activeResult = orderRepo.getActiveOrdersForRider(uid)

            val user = userResult.getOrNull()
            val available = readyResult.getOrNull() ?: emptyList()
            val myActive = activeResult.getOrNull()?.firstOrNull()

            _uiState.value = DeliveryUiState.Ready(
                welcomeName = user?.name ?: "Repartidor",
                completedDeliveries = user?.completedDeliveries ?: 0L,
                earnings = user?.earnings ?: 0L,
                availableOrders = available,
                activeOrder = myActive
            )
        }
    }

    fun acceptOrder(order: Order) {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            val userRes = userRepo.getUserProfile(uid)
            val rider = userRes.getOrNull()
            val riderName = rider?.name ?: "Repartidor"
            val riderPhone = rider?.phone ?: ""
            orderRepo.acceptForDelivery(order.id, uid, riderName, riderPhone).fold(
                onSuccess = { load() },
                onFailure = {
                    _uiState.value = DeliveryUiState.Error(
                        it.message ?: "No se pudo aceptar el pedido"
                    )
                    load()
                }
            )
        }
    }

    fun markDelivered(orderId: String) {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            orderRepo.updateStatus(orderId, "Entregado", "Pedido entregado")
            userRepo.addEarnings(uid, 8000L)
            load()
        }
    }

    fun markOnTheWay(orderId: String) {
        viewModelScope.launch {
            orderRepo.updateStatus(orderId, "En_camino", "Repartidor en camino a tu dirección")
            load()
        }
    }
}
