package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null

    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAdminDashboardBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        setupQuickActions()

        setupLogout()

        loadDashboardData()
    }

    // LOAD DASHBOARD

    private fun loadDashboardData() {
        loadProducts()
        loadOrders()
        loadUsers()
        loadLowStock()
        loadRecentOrders()
        loadTopProducts()
    }

    // PRODUCTS

    private fun loadProducts() {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { result ->
                val totalProducts = result.size()
                binding.adminProductsCount.text = totalProducts.toString()
                binding.adminProductsGrowth.text = "+${totalProducts}" // Placeholder growth
            }
    }

    // ORDERS

    private fun loadOrders() {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { result ->
                val totalOrders = result.size()
                binding.adminOrdersCount.text = totalOrders.toString()
                binding.adminOrdersGrowth.text = "+${(totalOrders * 0.1).toInt()}%"

                // TOTAL SALES
                var totalSales = 0L
                for (document in result.documents) {
                    totalSales += document.getLong("total") ?: 0L
                }

                val formatted = NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(totalSales)
                binding.adminSales.text = formatted
            }
    }

    // USERS

    private fun loadUsers() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val totalUsers = result.size()
                binding.adminUsersCount.text = totalUsers.toString()
                binding.adminUsersGrowth.text = "+${(totalUsers * 0.05).toInt()}%"
            }
    }

    // LOW STOCK

    private fun loadLowStock() {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    val presentations = document.get("presentations") as? Map<*, *>
                    presentations?.forEach { (_, value) ->
                        val presentation = value as? Map<*, *>
                        val stock = presentation?.get("stock").toString().toIntOrNull() ?: 0

                        if (stock <= 8) {
                            val productName = document.getString("name") ?: "Producto"
                            binding.adminLowStockText.text = "Stock bajo: $productName ($stock unidades)"
                            binding.alertStock.visibility = View.VISIBLE
                            return@addOnSuccessListener
                        }
                    }
                }
            }
    }

    // RECENT ORDERS

    private fun loadRecentOrders() {
        firestore.collection("orders")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { result ->
                binding.containerRecentOrders.removeAllViews()
                for (document in result.documents) {
                    addOrderView(document)
                }
            }
    }

    private fun addOrderView(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.item_recent_order_admin, binding.containerRecentOrders, false)
        
        val idText = layout.findViewById<android.widget.TextView>(R.id.orderId)
        val userText = layout.findViewById<android.widget.TextView>(R.id.orderUser)
        val statusText = layout.findViewById<android.widget.TextView>(R.id.orderStatus)
        val amountText = layout.findViewById<android.widget.TextView>(R.id.orderAmount)
        val timeText = layout.findViewById<android.widget.TextView>(R.id.orderTime)

        val id = doc.id.take(8).uppercase()
        idText.text = "#BT-$id"
        userText.text = doc.getString("userName") ?: "Cliente"
        
        val total = doc.getLong("total") ?: 0L
        amountText.text = NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(total)

        val status = doc.getString("status") ?: "Pendiente"
        statusText.text = status
        
        // Color status badge
        when (status.lowercase()) {
            "entregado" -> statusText.setBackgroundResource(R.drawable.bg_status_green)
            "preparando" -> statusText.setBackgroundResource(R.drawable.bg_status_yellow)
            else -> statusText.setBackgroundResource(R.drawable.bg_status_blue)
        }

        binding.containerRecentOrders.addView(layout)
    }

    // TOP PRODUCTS

    private fun loadTopProducts() {
        firestore.collection("products")
            .orderBy("salesCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { result ->
                binding.containerTopProducts.removeAllViews()
                for (document in result.documents) {
                    addProductView(document)
                }
            }
    }

    private fun addProductView(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.item_top_product_admin, binding.containerTopProducts, false)
        
        val nameText = layout.findViewById<android.widget.TextView>(R.id.productName)
        val salesText = layout.findViewById<android.widget.TextView>(R.id.productSales)
        val amountText = layout.findViewById<android.widget.TextView>(R.id.productAmount)
        val ratingText = layout.findViewById<android.widget.TextView>(R.id.productRating)

        nameText.text = doc.getString("name") ?: "Producto"
        val sales = doc.getLong("salesCount") ?: 0L
        salesText.text = "$sales ventas"
        
        // Use a placeholder amount if not present
        val revenue = sales * 45000 // Just for UI demonstration
        amountText.text = "$${revenue / 1000000.0}M"
        
        ratingText.text = "4.8" // Placeholder

        binding.containerTopProducts.addView(layout)
    }

    // QUICK ACTIONS

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

        binding.btnClients.setOnClickListener {
            Toast.makeText(requireContext(), "Gestión de clientes próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnReports.setOnClickListener {
            Toast.makeText(requireContext(), "Reportes detallados próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnViewAllOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminOrdersFragment())
                .addToBackStack(null).commit()
        }
    }

    // LOGOUT

    private fun setupLogout() {

        binding.btnLogoutAdmin.setOnClickListener {

            auth.signOut()

            Toast.makeText(
                requireContext(),
                "Sesión cerrada 👋",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    ProfileFragment()
                )
                .commit()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}