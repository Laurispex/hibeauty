package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentUserOrdersBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserOrdersFragment : Fragment() {

    private var _binding: FragmentUserOrdersBinding? = null
    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val orderAdapter =
        OrderAdapter(showStatusAction = false) { _ ->
            // No action needed for users, just tracking
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentUserOrdersBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        binding.userOrdersRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.userOrdersRecyclerView.adapter =
            orderAdapter

        // BACK BUTTON
        binding.btnBackOrders.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadUserOrders()
    }

    private fun loadUserOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.userOrdersEmptyText.isVisible = true
            binding.userOrdersEmptyText.text = "Inicia sesión para ver tus pedidos."
            binding.userOrdersRecyclerView.isVisible = false
            return
        }

        binding.userOrdersEmptyText.isVisible = true
        binding.userOrdersEmptyText.text = "Cargando tus pedidos..."
        binding.userOrdersRecyclerView.isVisible = false

        db.collection("orders")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener

                val orders = result.documents.map { document ->
                    documentToOrder(document.id, document.data.orEmpty())
                }

                if (orders.isEmpty()) {
                    binding.userOrdersEmptyText.isVisible = true
                    binding.userOrdersEmptyText.text = "Aún no tienes pedidos registrados. ¡Haz tu primera compra! 💖"
                    binding.userOrdersRecyclerView.isVisible = false
                    return@addOnSuccessListener
                }

                orderAdapter.submitList(orders)
                binding.userOrdersEmptyText.isVisible = false
                binding.userOrdersRecyclerView.isVisible = true
            }
            .addOnFailureListener { e ->
                // Si falla por índice de Firestore, intentar cargar sin ordenar por fecha
                loadUserOrdersWithoutSorting(currentUser.uid)
            }
    }

    private fun loadUserOrdersWithoutSorting(userId: String) {
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener

                val orders = result.documents.map { document ->
                    documentToOrder(document.id, document.data.orEmpty())
                }

                if (orders.isEmpty()) {
                    binding.userOrdersEmptyText.isVisible = true
                    binding.userOrdersEmptyText.text = "Aún no tienes pedidos registrados. ¡Haz tu primera compra! 💖"
                    binding.userOrdersRecyclerView.isVisible = false
                    return@addOnSuccessListener
                }

                // Ordenar localmente por ID o tiempo si existe
                val sortedOrders = orders.sortedByDescending { it.id }

                orderAdapter.submitList(sortedOrders)
                binding.userOrdersEmptyText.isVisible = false
                binding.userOrdersRecyclerView.isVisible = true
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.userOrdersEmptyText.isVisible = true
                binding.userOrdersEmptyText.text = "No se pudieron cargar los pedidos: ${e.message}"
                binding.userOrdersRecyclerView.isVisible = false
            }
    }

    private fun documentToOrder(
        documentId: String,
        data: Map<String, Any>
    ): Order {
        val rawItems = data["items"] as? List<*> ?: emptyList<Any>()
        val items = rawItems.mapNotNull { rawItem ->
            val item = rawItem as? Map<*, *> ?: return@mapNotNull null
            OrderItem(
                name = item["name"] as? String ?: "",
                presentation = item["presentation"] as? String ?: "",
                quantity = (item["quantity"] as? Number)?.toLong() ?: 0L,
                price = (item["price"] as? Number)?.toLong() ?: 0L
            )
        }

        return Order(
            id = data["id"] as? String ?: documentId,
            userId = data["userId"] as? String ?: "",
            status = data["status"] as? String ?: "Pendiente",
            total = (data["total"] as? Number)?.toLong() ?: 0L,
            items = items
        )
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
}
