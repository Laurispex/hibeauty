package com.example.hibeauty.ui.store.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class InventoryUiState {
    object Loading : InventoryUiState()
    object Empty : InventoryUiState()
    data class Ready(val products: List<Product>) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
}

class StoreInventoryViewModel(
    private val productRepo: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            productRepo.getAllForStore().fold(
                onSuccess = { products ->
                    _uiState.value =
                        if (products.isEmpty()) InventoryUiState.Empty
                        else InventoryUiState.Ready(products)
                },
                onFailure = { _uiState.value = InventoryUiState.Error("No se pudo cargar el inventario") }
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepo.deleteProduct(productId).fold(
                onSuccess = { load() },
                onFailure = { _uiState.value = InventoryUiState.Error("No se pudo eliminar el producto") }
            )
        }
    }
}
