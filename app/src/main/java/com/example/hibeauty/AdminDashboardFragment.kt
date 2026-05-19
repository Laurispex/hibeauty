package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentAdminDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

        auth = FirebaseAuth.getInstance()

        firestore = FirebaseFirestore.getInstance()

        setupQuickActions()

        setupLogout()

        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()

        activity
            ?.findViewById<BottomNavigationView>(
                R.id.bottom_navigation
            )
            ?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        activity
            ?.findViewById<BottomNavigationView>(
                R.id.bottom_navigation
            )
            ?.visibility = View.VISIBLE
    }

    private fun loadDashboardData() {

        loadProducts()

        loadOrders()

        loadUsers()

        loadLowStock()

        loadRecentOrders()

        loadTopProducts()
    }

    private fun loadProducts() {

        firestore.collection("products")
            .get()

            .addOnSuccessListener { result ->

                val total = result.size()

                binding.adminProductsCount.text =
                    total.toString()

                binding.adminProductsGrowth.text =
                    "+$total"
            }
    }

    private fun loadOrders() {

        firestore.collection("orders")
            .get()

            .addOnSuccessListener { result ->

                val totalOrders =
                    result.size()

                binding.adminOrdersCount.text =
                    totalOrders.toString()

                binding.adminOrdersGrowth.text =
                    "+${(totalOrders * 0.1).toInt()}%"

                var totalSales = 0L

                for (doc in result.documents) {

                    totalSales +=
                        doc.getLong("total")
                            ?: 0L
                }

                val formatted =
                    NumberFormat
                        .getCurrencyInstance(
                            Locale("es", "CO")
                        )
                        .format(totalSales)

                binding.adminSales.text =
                    formatted

                binding.adminSalesGrowth.text =
                    "+12%"
            }
    }

    private fun loadUsers() {

        firestore.collection("users")
            .get()

            .addOnSuccessListener { result ->

                val total =
                    result.size()

                binding.adminUsersCount.text =
                    total.toString()

                binding.adminUsersGrowth.text =
                    "+${(total * 0.05).toInt()}%"
            }
    }

    private fun loadLowStock() {

        firestore.collection("products")
            .get()

            .addOnSuccessListener { result ->

                for (doc in result.documents) {

                    val presentations =
                        doc.get("presentations")
                                as? Map<*, *>

                    presentations?.forEach { (_, value) ->

                        val p =
                            value as? Map<*, *>

                        val stock =
                            p?.get("stock")
                                .toString()
                                .toIntOrNull()
                                ?: 0

                        if (stock <= 8) {

                            val name =
                                doc.getString("name")
                                    ?: "Producto"

                            binding.adminLowStockText.text =
                                "Stock bajo: $name ($stock unidades)"

                            binding.alertStock.visibility =
                                View.VISIBLE

                            return@addOnSuccessListener
                        }
                    }
                }
            }
    }

    private fun loadRecentOrders() {

        firestore.collection("orders")
            .orderBy(
                "timestamp",
                Query.Direction.DESCENDING
            )
            .limit(3)
            .get()

            .addOnSuccessListener { result ->

                binding.containerRecentOrders
                    .removeAllViews()

                for (doc in result.documents) {

                    addOrderView(doc)
                }
            }
    }

    private fun addOrderView(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {

        val layout =
            LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.item_recent_order_admin,
                    binding.containerRecentOrders,
                    false
                )

        layout.findViewById<TextView>(R.id.orderId).text =
            "#BT-${doc.id.take(6).uppercase()}"

        layout.findViewById<TextView>(R.id.orderUser).text =
            doc.getString("userName")
                ?: "Cliente"

        val total =
            doc.getLong("total")
                ?: 0L

        layout.findViewById<TextView>(R.id.orderAmount).text =
            NumberFormat
                .getCurrencyInstance(
                    Locale("es", "CO")
                )
                .format(total)

        val status =
            doc.getString("status")
                ?: "Pendiente"

        val statusText =
            layout.findViewById<TextView>(
                R.id.orderStatus
            )

        statusText.text = status

        when (status.lowercase()) {

            "entregado" ->
                statusText.setBackgroundResource(
                    R.drawable.bg_status_green
                )

            "preparando" ->
                statusText.setBackgroundResource(
                    R.drawable.bg_status_yellow
                )

            else ->
                statusText.setBackgroundResource(
                    R.drawable.bg_status_blue
                )
        }

        binding.containerRecentOrders
            .addView(layout)
    }

    private fun loadTopProducts() {

        firestore.collection("products")
            .orderBy(
                "salesCount",
                Query.Direction.DESCENDING
            )
            .limit(3)
            .get()

            .addOnSuccessListener { result ->

                binding.containerTopProducts
                    .removeAllViews()

                for (doc in result.documents) {

                    addProductView(doc)
                }
            }
    }

    private fun addProductView(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {

        val layout =
            LayoutInflater.from(requireContext())
                .inflate(
                    R.layout.item_top_product_admin,
                    binding.containerTopProducts,
                    false
                )

        layout.findViewById<TextView>(R.id.productName).text =
            doc.getString("name")
                ?: "Producto"

        val sales =
            doc.getLong("salesCount")
                ?: 0L

        layout.findViewById<TextView>(R.id.productSales).text =
            "$sales ventas"

        val revenue =
            sales * 45000

        layout.findViewById<TextView>(R.id.productAmount).text =
            "$${revenue / 1000000.0}M"

        layout.findViewById<TextView>(R.id.productRating).text =
            "⭐ 4.8"

        binding.containerTopProducts
            .addView(layout)
    }

    private fun setupQuickActions() {

        binding.btnInventory.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    AdminInventoryFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.btnOrders.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    AdminOrdersFragment()
                )
                .addToBackStack(null)
                .commit()
        }



        binding.btnViewAllOrders.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    AdminOrdersFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupLogout() {

        binding.btnLogoutAdmin
            .setOnClickListener {

                auth.signOut()

                Toast.makeText(
                    requireContext(),
                    "Sesión cerrada 👋",
                    Toast.LENGTH_SHORT
                ).show()

                val mainActivity = requireActivity() as? MainActivity
                mainActivity?.showUserNavigation()
                mainActivity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
            }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}