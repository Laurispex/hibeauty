package com.example.hibeauty

import com.example.hibeauty.data.model.Product

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.databinding.FragmentDeliveryDashboardBinding
import com.example.hibeauty.ui.delivery.DeliveryUiState
import com.example.hibeauty.ui.delivery.DeliveryViewModel
import com.example.hibeauty.util.toCOP
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class DeliveryDashboardFragment : Fragment() {

    private var _binding: FragmentDeliveryDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeliveryViewModel by viewModels()

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveryDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLogout()
        setupAvailabilitySwitch()
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
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DeliveryUiState.Loading -> showLoading()
                        is DeliveryUiState.Ready   -> renderState(state)
                        is DeliveryUiState.Error   -> toast(state.message)
                    }
                }
            }
        }
    }

    // ─── UI STATES ─────────────────────────────────────────────────────────────

    private fun showLoading() {
        binding.deliveryWelcomeText.text = "Cargando..."
        binding.textCompletedDeliveries.text = "-"
        binding.textTotalEarnings.text = "-"
    }

    private fun renderState(state: DeliveryUiState.Ready) {
        val b = _binding ?: return

        b.deliveryWelcomeText.text = "¡Hola, ${state.welcomeName}!"
        b.textCompletedDeliveries.text = state.completedDeliveries.toString()
        b.textTotalEarnings.text = state.earnings.toCOP()

        if (state.activeOrder != null) {
            showActiveDeliveryHUD(state.activeOrder)
        } else {
            b.cardActiveDelivery.isVisible = false
            renderAvailableOrders(state.availableOrders)
        }
    }

    private fun renderAvailableOrders(orders: List<Order>) {
        val b = _binding ?: return
        b.containerAvailableOrders.removeAllViews()

        if (orders.isEmpty()) {
            b.layoutEmptyDeliveries.isVisible = true
            b.layoutAvailableDeliveries.isVisible = false
        } else {
            b.layoutEmptyDeliveries.isVisible = false
            b.layoutAvailableDeliveries.isVisible = true
            orders.forEach { order -> inflateOrderCard(order) }
        }
    }

    private fun showActiveDeliveryHUD(order: Order) {
        val b = _binding ?: return
        b.layoutEmptyDeliveries.isVisible = false
        b.layoutAvailableDeliveries.isVisible = false
        b.cardActiveDelivery.isVisible = true

        b.activeOrderId.text = "#BT-${order.id.take(6).uppercase()}"
        b.activeClientName.text = order.userName.ifBlank { "Cliente HiBeauty" }
        b.activeClientAddress.text = order.address.ifBlank { "Dirección de entrega" }
        b.activeClientPhone.text = "Teléfono: ${order.userPhone}"

        val itemsSummary = order.items.joinToString { "${it.quantity}x ${it.name}" }
            .ifBlank { "Productos de Belleza" }
        b.activeOrderItemsSummary.text = "Productos: $itemsSummary"

        val isOnTheWay = order.status.lowercase().contains("camino")
        b.btnUpdateDeliveryStatus.text =
            if (isOnTheWay) "Marcar como Entregado" else "Marcar como En Camino"
        b.btnUpdateDeliveryStatus.setOnClickListener {
            if (isOnTheWay) viewModel.markDelivered(order.id)
            else viewModel.markOnTheWay(order.id)
        }

        b.btnCallClient.setOnClickListener {
            val phone = order.userPhone
            if (phone.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") })
            } else {
                toast("El cliente no registró número de teléfono")
            }
        }
    }

    private fun inflateOrderCard(order: Order) {
        val b = _binding ?: return
        val layout = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_delivery_order, b.containerAvailableOrders, false)

        layout.findViewById<TextView>(R.id.orderIdText).text = "#BT-${order.id.take(6).uppercase()}"
        layout.findViewById<TextView>(R.id.payoutText).text = "Pago: ${order.shipping.toCOP()}"
        layout.findViewById<TextView>(R.id.destinationAddressText).text =
            order.address.ifBlank { "Dirección no especificada" }

        val timeToStore = calculateSimulatedTime(order.id, 1)
        val timeToClient = calculateSimulatedTime(order.id, 2)
        layout.findViewById<TextView>(R.id.timeToStoreText).text = "Tiempo a tienda: $timeToStore min"
        layout.findViewById<TextView>(R.id.timeToClientText).text = "Tiempo al cliente: $timeToClient min"

        val summary = order.items.joinToString { "${it.quantity}x ${it.name}" }.ifBlank { "Productos de Belleza" }
        layout.findViewById<TextView>(R.id.itemsSummaryText).text = "Productos: $summary"

        layout.findViewById<Button>(R.id.btnAcceptOrder).setOnClickListener {
            viewModel.acceptOrder(order)
        }
        b.containerAvailableOrders.addView(layout)
    }

    // ─── SETUP ─────────────────────────────────────────────────────────────────

    private fun setupAvailabilitySwitch() {
        binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            binding.textAvailabilityStatus.text =
                if (isChecked) "Estado: Conectado (Disponible)" else "Estado: Desconectado (Offline)"
            if (isChecked) viewModel.load() else {
                binding.layoutEmptyDeliveries.isVisible = true
                binding.layoutAvailableDeliveries.isVisible = false
                binding.cardActiveDelivery.isVisible = false
            }
        }
    }

    private fun setupLogout() {
        binding.btnLogoutDelivery.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            toast("Sesión cerrada")
            val main = requireActivity() as? MainActivity
            main?.showUserNavigation()
            main?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_home
        }
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    /** Deterministic pseudo-random time (5–30 min) based on order ID. */
    private fun calculateSimulatedTime(id: String, salt: Int): Int =
        kotlin.math.abs((id.hashCode() + salt) % 26) + 5

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
