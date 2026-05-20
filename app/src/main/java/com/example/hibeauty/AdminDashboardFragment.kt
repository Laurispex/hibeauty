package com.example.hibeauty

import com.example.hibeauty.data.model.Product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.databinding.FragmentAdminDashboardBinding
import com.example.hibeauty.ui.store.dashboard.StoreDashboardData
import com.example.hibeauty.ui.store.dashboard.StoreDashboardViewModel
import com.example.hibeauty.util.toCOP
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StoreDashboardViewModel by viewModels()

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuickActions()
        setupLogout()
        observeViewModel()
        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── OBSERVERS ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    // Could show a progress bar here if needed
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collect { data -> renderDashboard(data) }
            }
        }
    }

    // ─── RENDER ────────────────────────────────────────────────────────────────

    private fun renderDashboard(data: StoreDashboardData) {
        val b = _binding ?: return

        b.adminWelcomeText.text = "¡Hola, ${data.welcomeName}!"
        b.adminSales.text = data.totalSales.toCOP()
        b.adminSalesGrowth.text = "+${(data.ordersCount * 0.1).toInt()}%"
        b.adminOrdersCount.text = data.ordersCount.toString()
        b.adminOrdersGrowth.text = "+${(data.ordersCount * 0.1).toInt()}%"
        b.adminUsersCount.text = data.usersCount.toString()
        b.adminUsersGrowth.text = "+${(data.usersCount * 0.05).toInt()}%"
        b.adminProductsCount.text = data.productsCount.toString()
        b.adminProductsGrowth.text = "+${data.productsCount}"

        // Low-stock alert
        val warning = data.lowStockWarning
        if (warning != null) {
            b.adminLowStockText.text = warning
            b.alertStock.isVisible = true
        } else {
            b.alertStock.isVisible = false
        }

        // Recent orders
        b.containerRecentOrders.removeAllViews()
        data.recentOrders.forEach { order -> addOrderView(order) }

        // Top products
        b.containerTopProducts.removeAllViews()
        data.topProducts.forEach { product ->
            val layout = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_top_product_admin, b.containerTopProducts, false)
            layout.findViewById<TextView>(R.id.productName).text = product.name
            val totalStock = product.presentations.values.sumOf { it.stock }
            layout.findViewById<TextView>(R.id.productSales).text = "$totalStock en stock"
            val revenue = product.presentations.values.sumOf { it.price * it.stock }
            layout.findViewById<TextView>(R.id.productAmount).text = revenue.toCOP()
            b.containerTopProducts.addView(layout)
        }
    }

    private fun addOrderView(order: Order) {
        val b = _binding ?: return
        val layout = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_recent_order_admin, b.containerRecentOrders, false)

        layout.findViewById<TextView>(R.id.orderId).text = "#BT-${order.id.take(6).uppercase()}"
        layout.findViewById<TextView>(R.id.orderUser).text = order.userName.ifBlank { "Cliente" }
        layout.findViewById<TextView>(R.id.orderAmount).text = order.total.toCOP()
        layout.findViewById<TextView>(R.id.orderTime).text = formatElapsed(order.createdAtMillis)

        val statusView = layout.findViewById<TextView>(R.id.orderStatus)
        statusView.text = order.statusLabel.ifBlank { order.status }
        statusView.setBackgroundResource(
            when (order.status.lowercase()) {
                "entregado" -> R.drawable.bg_status_green
                "preparando" -> R.drawable.bg_status_yellow
                else -> R.drawable.bg_status_blue
            }
        )
        b.containerRecentOrders.addView(layout)
    }

    private fun formatElapsed(millis: Long): String {
        if (millis == 0L) return "Reciente"
        val diff = System.currentTimeMillis() - millis
        val minutes = diff / 60_000
        return when {
            minutes < 1    -> "Hace un momento"
            minutes < 60   -> "Hace ${minutes} min"
            minutes < 1440 -> "Hace ${minutes / 60}h"
            else           -> "Hace ${minutes / 1440}d"
        }
    }

    // ─── ACTIONS ───────────────────────────────────────────────────────────────

    private fun setupQuickActions() {
        binding.btnInventory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminInventoryFragment())
                .addToBackStack(null).commit()
        }
        binding.btnOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminOrdersFragment())
                .addToBackStack(null).commit()
        }
        binding.btnViewAllOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminOrdersFragment())
                .addToBackStack(null).commit()
        }
    }

    private fun setupLogout() {
        binding.btnLogoutAdmin.setOnClickListener {
            viewModel.logout()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val main = requireActivity() as? MainActivity
            main?.showUserNavigation()
            main?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_home
        }
    }
}
