package com.example.hibeauty.ui.store.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.repository.OrderRepository
import com.example.hibeauty.data.repository.ProductRepository
import com.example.hibeauty.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class StoreDashboardData(
    val welcomeName: String = "",
    val totalSales: Long = 0L,
    val ordersCount: Int = 0,
    val usersCount: Int = 0,
    val productsCount: Int = 0,
    val recentOrders: List<Order> = emptyList(),
    val topProducts: List<Product> = emptyList(),
    val lowStockWarning: String? = null
)

class StoreDashboardViewModel(
    private val productRepo: ProductRepository = ProductRepository(),
    private val orderRepo: OrderRepository = OrderRepository(),
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _data = MutableStateFlow(StoreDashboardData())
    val data: StateFlow<StoreDashboardData> = _data

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun load() {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val db = FirebaseFirestore.getInstance()

            val nameResult = runCatching { db.collection("users").document(uid).get().await().getString("name") ?: "Tienda" }
            val productsResult = productRepo.getAllForStore()
            val ordersResult = orderRepo.getRecentOrders(10)
            val usersCount = runCatching { db.collection("users").whereEqualTo("role", "user").get().await().size() }.getOrNull() ?: 0

            val products = productsResult.getOrNull() ?: emptyList()
            val orders = ordersResult.getOrNull() ?: emptyList()
            val totalSales = orders.sumOf { it.total }

            val lowStockProduct = products.firstOrNull { p ->
                p.presentations.values.any { it.stock in 1..4 }
            }

            _data.value = StoreDashboardData(
                welcomeName = nameResult.getOrNull() ?: "Tienda",
                totalSales = totalSales,
                ordersCount = orders.size,
                usersCount = usersCount,
                productsCount = products.size,
                recentOrders = orders.take(5),
                topProducts = products.sortedByDescending { it.presentations.values.sumOf { p -> p.stock } }.take(5),
                lowStockWarning = lowStockProduct?.let { "${it.name} tiene stock bajo" }
            )
            _isLoading.value = false
        }
    }

    fun logout() = userRepo.logout()
}
