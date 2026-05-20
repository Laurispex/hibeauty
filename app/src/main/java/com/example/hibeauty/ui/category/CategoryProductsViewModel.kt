package com.example.hibeauty.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoryProductsViewModel(
    private val productRepo: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    fun loadAllProducts(category: String) {
        viewModelScope.launch {
            productRepo.getProductsByCategory(category).onSuccess { list ->
                _products.value = list
            }
        }
    }

    fun loadFeaturedProducts(category: String) {
        viewModelScope.launch {
            productRepo.getProductsByCategory(category).onSuccess { list ->
                _products.value = list.filter { it.isFeatured }
            }
        }
    }

    fun loadNewProducts(category: String) {
        viewModelScope.launch {
            productRepo.getProductsByCategory(category).onSuccess { list ->
                _products.value = list.filter { it.isNew }
            }
        }
    }

    fun loadOfferProducts(category: String) {
        viewModelScope.launch {
            productRepo.getProductsByCategory(category).onSuccess { list ->
                _products.value = list.filter { it.isOffer }
            }
        }
    }
}
