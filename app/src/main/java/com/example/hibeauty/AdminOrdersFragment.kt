package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentAdminOrdersBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AdminOrdersFragment : Fragment() {

    private var _binding: FragmentAdminOrdersBinding? = null

    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val orderAdapter =
        OrderAdapter(showStatusAction = true) { order ->
            updateOrderStatus(order)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAdminOrdersBinding.inflate(
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

        binding.adminOrdersRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.adminOrdersRecyclerView.adapter =
            orderAdapter

        // BACK BUTTON

        binding.btnBackOrders.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        loadOrders()
    }

    private fun loadOrders() {

        binding.adminOrdersEmptyText.isVisible =
            true

        binding.adminOrdersEmptyText.text =
            "Cargando pedidos..."

        binding.adminOrdersRecyclerView.isVisible =
            false

        db.collection("orders")
            .get()

            .addOnSuccessListener { result ->

                val orders =
                    result.documents.map { document ->

                        documentToOrder(
                            document.id,
                            document.data.orEmpty()
                        )
                    }

                if (orders.isEmpty()) {

                    binding.adminOrdersEmptyText.isVisible =
                        true

                    binding.adminOrdersEmptyText.text =
                        "Todavía no hay pedidos."

                    binding.adminOrdersRecyclerView.isVisible =
                        false

                    return@addOnSuccessListener
                }

                orderAdapter.submitList(orders)

                binding.adminOrdersEmptyText.isVisible =
                    false

                binding.adminOrdersRecyclerView.isVisible =
                    true
            }

            .addOnFailureListener {

                binding.adminOrdersEmptyText.isVisible =
                    true

                binding.adminOrdersEmptyText.text =
                    "No se pudieron cargar los pedidos."

                binding.adminOrdersRecyclerView.isVisible =
                    false
            }
    }

    private fun updateOrderStatus(
        order: Order
    ) {

        val nextStatus = when (order.status) {

            "Pendiente" ->
                "Preparando"

            "Preparando" ->
                "Listo"

            "Listo" ->
                "Domicilio Propio"

            "Domicilio Propio" ->
                "Entregado"

            else -> null
        }

        if (nextStatus == null) {

            toast("Este pedido ya está finalizado")

            return
        }

        db.collection("orders")
            .document(order.id)

            .update(
                mapOf(

                    "status" to nextStatus,

                    "statusLabel" to
                            statusLabel(nextStatus),

                    "statusUpdatedAt" to
                            FieldValue.serverTimestamp(),

                    "statusHistory" to
                            FieldValue.arrayUnion(

                                hashMapOf(

                                    "status" to nextStatus,

                                    "label" to
                                            statusLabel(nextStatus),

                                    "changedAtMillis" to
                                            System.currentTimeMillis()
                                )
                            )
                )
            )

            .addOnSuccessListener {

                toast(
                    "Pedido actualizado a $nextStatus"
                )

                loadOrders()
            }

            .addOnFailureListener {

                toast(
                    "No se pudo actualizar el pedido"
                )
            }
    }

    private fun documentToOrder(
        documentId: String,
        data: Map<String, Any>
    ): Order {

        val rawItems =
            data["items"] as? List<*>
                ?: emptyList<Any>()

        val items =
            rawItems.mapNotNull { rawItem ->

                val item =
                    rawItem as? Map<*, *>
                        ?: return@mapNotNull null

                OrderItem(

                    name =
                        item["name"] as? String
                            ?: "",

                    presentation =
                        item["presentation"] as? String
                            ?: "",

                    quantity =
                        (item["quantity"] as? Number)
                            ?.toLong()
                            ?: 0L,

                    price =
                        (item["price"] as? Number)
                            ?.toLong()
                            ?: 0L
                )
            }

        return Order(

            id =
                data["id"] as? String
                    ?: documentId,

            userId =
                data["userId"] as? String
                    ?: "",

            status =
                data["status"] as? String
                    ?: "Pendiente",

            total =
                (data["total"] as? Number)
                    ?.toLong()
                    ?: 0L,

            items = items
        )
    }

    private fun statusLabel(
        status: String
    ): String {

        return when (status) {

            "Preparando" ->
                "Preparando tu compra"

            "Listo" ->
                "Listo para despacho"

            "Domicilio Propio" ->
                "En ruta directa de tienda"

            "Entregado" ->
                "Pedido entregado"

            else ->
                "Pedido recibido"
        }
    }

    private fun toast(
        message: String
    ) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
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

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}