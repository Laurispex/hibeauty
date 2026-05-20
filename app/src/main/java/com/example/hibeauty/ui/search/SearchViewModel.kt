package com.example.hibeauty.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val productRepo: ProductRepository = ProductRepository()
) : ViewModel() {

    private val allProducts = mutableListOf<Product>()
    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts

    fun loadProducts() {
        viewModelScope.launch {
            productRepo.getActiveProducts().onSuccess { list ->
                allProducts.clear()
                allProducts.addAll(list)
                _filteredProducts.value = list
            }
        }
    }

    fun filter(query: String) {
        if (query.isBlank()) {
            _filteredProducts.value = allProducts.toList()
            return
        }
        val q = query.lowercase()
        _filteredProducts.value = allProducts.filter {
            it.name.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.benefits.lowercase().contains(q)
        }
    }
}
