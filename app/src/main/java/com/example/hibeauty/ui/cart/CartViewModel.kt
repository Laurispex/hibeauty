package com.example.hibeauty.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.repository.CartRepository
import com.example.hibeauty.data.repository.OrderRepository
import com.example.hibeauty.data.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CartUiState {
    object Loading : CartUiState()
    object Empty : CartUiState()
    object NotLoggedIn : CartUiState()
    data class Ready(val items: List<CartItem>, val subtotal: Long, val shipping: Long, val total: Long) : CartUiState()
    data class Error(val message: String) : CartUiState()
}

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Loading : CheckoutState()
    data class Success(val orderId: String) : CheckoutState()
    data class StockError(val message: String) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

class CartViewModel(
    private val cartRepo: CartRepository = CartRepository(),
    private val productRepo: ProductRepository = ProductRepository(),
    private val orderRepo: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _cartState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val cartState: StateFlow<CartUiState> = _cartState

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    fun loadCart(userId: String?) {
        if (userId == null) { _cartState.value = CartUiState.NotLoggedIn; return }
        viewModelScope.launch {
            _cartState.value = CartUiState.Loading
            cartRepo.getCartItems(userId).fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        _cartState.value = CartUiState.Empty
                    } else {
                        val subtotal = items.sumOf { it.price * it.quantity }
                        val shipping = 8_000L
                        _cartState.value = CartUiState.Ready(items, subtotal, shipping, subtotal + shipping)
                    }
                },
                onFailure = { _cartState.value = CartUiState.Error("No se pudo cargar el carrito") }
            )
        }
    }

    fun removeItem(userId: String, item: CartItem) {
        viewModelScope.launch {
            cartRepo.removeItem(userId, item.id)
            loadCart(userId)
        }
    }

    fun checkout(userId: String, items: List<CartItem>, userName: String, userPhone: String, address: String) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()
            val checkedItems = mutableListOf<CartItem>()

            for (item in items) {
                val result = productRepo.getProductById(item.productId)
                result.onFailure {
                    _checkoutState.value = CheckoutState.Error("Error validando stock")
                    return@launch
                }
                val product = result.getOrThrow()
                val presentationData = product.presentations[item.presentation]
                val currentStock = presentationData?.stock ?: 0L
                if (currentStock < item.quantity) {
                    _checkoutState.value = CheckoutState.StockError("Sin stock: ${item.name} (${item.presentation})")
                    return@launch
                }
                batch.update(
                    db.collection("products").document(item.productId),
                    "presentations.${item.presentation}.stock",
                    currentStock - item.quantity
                )
                checkedItems.add(item)
            }

            val subtotal = checkedItems.sumOf { it.price * it.quantity }
            val shipping = 8_000L
            val total = subtotal + shipping
            val earnedPoints = (subtotal / 1000L).coerceAtLeast(1L)

            val orderData = hashMapOf<String, Any>(
                "userId" to userId,
                "userName" to userName,
                "userPhone" to userPhone,
                "address" to address,
                "paymentMethod" to "Contraentrega",
                "status" to "Pendiente",
                "statusLabel" to "Pedido recibido",
                "subtotal" to subtotal,
                "shipping" to shipping,
                "total" to total,
                "earnedPoints" to earnedPoints,
                "createdAtMillis" to System.currentTimeMillis(),
                "items" to checkedItems.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "name" to item.name,
                        "imageUrl" to item.imageUrl,
                        "presentation" to item.presentation,
                        "price" to item.price,
                        "quantity" to item.quantity
                    )
                },
                "statusHistory" to listOf(
                    hashMapOf("status" to "Pendiente", "label" to "Pedido recibido", "changedAtMillis" to System.currentTimeMillis())
                )
            )

            orderRepo.createOrder(userId, orderData, checkedItems.map { it.id }, batch, earnedPoints).fold(
                onSuccess = { orderId -> _checkoutState.value = CheckoutState.Success(orderId) },
                onFailure = { _checkoutState.value = CheckoutState.Error("No se pudo crear el pedido") }
            )
        }
    }

    fun resetCheckout() { _checkoutState.value = CheckoutState.Idle }
}
