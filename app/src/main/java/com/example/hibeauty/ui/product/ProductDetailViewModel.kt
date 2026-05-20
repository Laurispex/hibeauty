package com.example.hibeauty.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.repository.CartRepository
import com.example.hibeauty.data.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val cartRepo: CartRepository = CartRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    fun addToCart(product: com.example.hibeauty.data.model.Product, selectedSize: String) {
        val user = userRepo.currentFirebaseUser
        if (user == null) {
            _actionResult.value = "Inicia sesión para comprar"
            return
        }

        val presentation = product.presentations[selectedSize]
        if (presentation == null || presentation.stock <= 0) {
            _actionResult.value = "Esta presentación no tiene stock"
            return
        }

        val cartItem = hashMapOf(
            "productId" to product.id,
            "name" to product.name,
            "imageUrl" to product.imageUrl,
            "presentation" to selectedSize,
            "price" to presentation.price,
            "quantity" to 1L,
            "createdAt" to FieldValue.serverTimestamp()
        )

        viewModelScope.launch {
            cartRepo.addItemToCart(user.uid, cartItem).fold(
                onSuccess = { _actionResult.value = "Producto agregado al carrito 💖" },
                onFailure = { _actionResult.value = "No se pudo agregar al carrito" }
            )
        }
    }

    fun clearResult() { _actionResult.value = null }
}
