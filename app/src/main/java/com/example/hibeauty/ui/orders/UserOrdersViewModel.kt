package com.example.hibeauty.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.repository.OrderRepository
import com.example.hibeauty.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UserOrdersUiState {
    object Loading : UserOrdersUiState()
    object NotLoggedIn : UserOrdersUiState()
    object Empty : UserOrdersUiState()
    data class Ready(val orders: List<Order>) : UserOrdersUiState()
    data class Error(val message: String) : UserOrdersUiState()
}

class UserOrdersViewModel(
    private val orderRepo: OrderRepository = OrderRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserOrdersUiState>(UserOrdersUiState.Loading)
    val uiState: StateFlow<UserOrdersUiState> = _uiState

    fun load() {
        val uid = userRepo.currentFirebaseUser?.uid
        if (uid == null) { _uiState.value = UserOrdersUiState.NotLoggedIn; return }

        viewModelScope.launch {
            _uiState.value = UserOrdersUiState.Loading
            orderRepo.getOrdersByUser(uid).fold(
                onSuccess = { orders ->
                    _uiState.value =
                        if (orders.isEmpty()) UserOrdersUiState.Empty
                        else UserOrdersUiState.Ready(orders)
                },
                onFailure = { _uiState.value = UserOrdersUiState.Error("No se pudieron cargar tus pedidos") }
            )
        }
    }
}
