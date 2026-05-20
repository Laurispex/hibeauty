package com.example.hibeauty.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.repository.ProductRepository
import com.example.hibeauty.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Ready(val featured: List<Product>, val newArrivals: List<Product>, val offers: List<Product>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val productRepo: ProductRepository = ProductRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _greeting = MutableStateFlow("Descubre tu rutina ideal")
    val greeting: StateFlow<String> = _greeting

    init {
        loadGreeting()
        loadProducts()
    }

    private fun loadGreeting() {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            userRepo.getUserProfile(uid).onSuccess { user ->
                _greeting.value = if (user.name.isNotBlank()) "Hola, ${user.name}" else "Descubre tu rutina ideal"
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            productRepo.getActiveProducts().fold(
                onSuccess = { products ->
                    _uiState.value = HomeUiState.Ready(
                        featured = products.filter { it.isFeatured },
                        newArrivals = products.filter { it.isNew },
                        offers = products.filter { it.isOffer }
                    )
                },
                onFailure = { _uiState.value = HomeUiState.Error("Error cargando productos") }
            )
        }
    }
}
