package com.example.hibeauty.ui.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.repository.OrderRepository
import com.example.hibeauty.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            val readyResult = orderRepo.getOrdersByStatus("Listo")
            val activeResult = orderRepo.getOrdersByStatus("En camino")

            val user = userResult.getOrNull()
            val available = readyResult.getOrNull() ?: emptyList()
            val myActive = activeResult.getOrNull()?.firstOrNull { it.riderId == uid }

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
        val db = FirebaseFirestore.getInstance()
        viewModelScope.launch {
            runCatching {
                val userDoc = db.collection("users").document(uid).get().await()
                val riderName = userDoc.getString("name") ?: "Repartidor"
                orderRepo.assignRider(order.id, uid, riderName)
                orderRepo.updateStatus(order.id, "En camino", "Pedido en camino")
            }
            load()
        }
    }

    fun markDelivered(orderId: String) {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            orderRepo.updateStatus(orderId, "Entregado", "Pedido entregado")
            runCatching {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(uid).update(
                    mapOf(
                        "completedDeliveries" to com.google.firebase.firestore.FieldValue.increment(1),
                        "earnings" to com.google.firebase.firestore.FieldValue.increment(8000L)
                    )
                ).await()
            }
            load()
        }
    }
}
