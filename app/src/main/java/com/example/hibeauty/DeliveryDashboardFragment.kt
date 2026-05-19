package com.example.hibeauty

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentDeliveryDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale

class DeliveryDashboardFragment : Fragment() {

    private var _binding: FragmentDeliveryDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var currentRiderName: String = "Repartidor"
    private var currentRiderPhone: String = ""

    private var activeDeliveryListener: ListenerRegistration? = null
    private var availableOrdersListener: ListenerRegistration? = null
    private var riderStatsListener: ListenerRegistration? = null

    private var activeOrderDoc: DocumentSnapshot? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveryDashboardBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupLogout()
        loadRiderProfileAndStart()
    }

    override fun onResume() {
        super.onResume()
        // Hide client navigation bar when in delivery dashboard for cleaner UX
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    private fun loadRiderProfileAndStart() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                currentRiderName = doc.getString("name") ?: "Repartidor"
                currentRiderPhone = doc.getString("phone") ?: ""
                binding.deliveryWelcomeText.text = "¡Hola, $currentRiderName!"

                // Start observing rider stats
                observeRiderStats(currentUser.uid)

                // Setup Availability Switch
                binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        binding.textAvailabilityStatus.text = "Estado: Conectado (Disponible)"
                        startObservingDeliveries(currentUser.uid)
                    } else {
                        binding.textAvailabilityStatus.text = "Estado: Desconectado (Offline)"
                        stopObservingDeliveries()
                        binding.layoutEmptyDeliveries.visibility = View.VISIBLE
                        binding.layoutAvailableDeliveries.visibility = View.GONE
                        binding.cardActiveDelivery.visibility = View.GONE
                    }
                }

                // Initial load
                if (binding.switchAvailability.isChecked) {
                    startObservingDeliveries(currentUser.uid)
                }
            }
            .addOnFailureListener {
                toast("Error cargando perfil de repartidor")
            }
    }

    private fun observeRiderStats(riderUid: String) {
        riderStatsListener?.remove()
        riderStatsListener = db.collection("users").document(riderUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val completed = snapshot.getLong("completedDeliveries") ?: 0L
                    val earnings = snapshot.getLong("earnings") ?: 0L

                    binding.textCompletedDeliveries.text = completed.toString()
                    binding.textTotalEarnings.text = earnings.toCOP()
                }
            }
    }

    private fun startObservingDeliveries(riderUid: String) {
        stopObservingDeliveries()

        // 1. Listen for active order assigned to this rider
        activeDeliveryListener = db.collection("orders")
            .whereEqualTo("riderId", riderUid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                // Find active delivery (not delivered yet)
                val activeOrder = snapshots?.documents?.find { doc ->
                    val status = doc.getString("status")?.lowercase() ?: ""
                    status != "entregado"
                }

                if (activeOrder != null) {
                    activeOrderDoc = activeOrder
                    showActiveDeliveryHUD(activeOrder)
                } else {
                    activeOrderDoc = null
                    binding.cardActiveDelivery.visibility = View.GONE
                    
                    // If no active delivery, listen for available deliveries in region
                    observeAvailableOrders()
                }
            }
    }

    private fun observeAvailableOrders() {
        availableOrdersListener?.remove()
        // Listen to orders preparing or ready, but not yet accepted by any rider
        availableOrdersListener = db.collection("orders")
            .whereIn("status", listOf("Preparando", "Listo para recoger", "Listo"))
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val availableOrders = snapshots?.documents?.filter { doc ->
                    doc.getString("riderId") == null
                } ?: emptyList()

                binding.containerAvailableOrders.removeAllViews()

                if (availableOrders.isEmpty()) {
                    binding.layoutEmptyDeliveries.visibility = View.VISIBLE
                    binding.layoutAvailableDeliveries.visibility = View.GONE
                } else {
                    binding.layoutEmptyDeliveries.visibility = View.GONE
                    binding.layoutAvailableDeliveries.visibility = View.VISIBLE

                    availableOrders.forEach { doc ->
                        inflateAvailableOrderCard(doc)
                    }
                }
            }
    }

    private fun inflateAvailableOrderCard(doc: DocumentSnapshot) {
        val layout = LayoutInflater.from(requireContext()).inflate(
            R.layout.item_delivery_order,
            binding.containerAvailableOrders,
            false
        )

        layout.findViewById<TextView>(R.id.orderIdText).text = "#BT-${doc.id.take(6).uppercase()}"
        
        val shipping = doc.getLong("shipping") ?: 8000L
        layout.findViewById<TextView>(R.id.payoutText).text = "Pago: ${shipping.toCOP()}"
        
        val address = doc.getString("address") ?: "Dirección no especificada"
        layout.findViewById<TextView>(R.id.destinationAddressText).text = address

        // Summarize items
        val itemsList = doc.get("items") as? List<Map<*, *>>
        val itemsSummary = itemsList?.joinToString { item ->
            val qty = item["quantity"] ?: 1
            val name = item["name"] ?: "Producto"
            "${qty}x $name"
        } ?: "Productos de Belleza"
        layout.findViewById<TextView>(R.id.itemsSummaryText).text = "Productos: $itemsSummary"

        // Accept Button
        layout.findViewById<Button>(R.id.btnAcceptOrder).setOnClickListener {
            acceptOrder(doc.id)
        }

        binding.containerAvailableOrders.addView(layout)
    }

    private fun acceptOrder(orderId: String) {
        val riderUid = auth.currentUser?.uid ?: return
        
        val updates = hashMapOf(
            "status" to "Aceptado",
            "statusLabel" to "Repartidor en camino a la tienda",
            "riderId" to riderUid,
            "riderName" to currentRiderName,
            "riderPhone" to currentRiderPhone,
            "statusUpdatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("orders").document(orderId).update(updates as Map<String, Any>)
            .addOnSuccessListener {
                toast("¡Pedido aceptado! Dirígete a la tienda")
            }
            .addOnFailureListener {
                toast("Error al aceptar pedido")
            }
    }

    private fun showActiveDeliveryHUD(doc: DocumentSnapshot) {
        binding.layoutEmptyDeliveries.visibility = View.GONE
        binding.layoutAvailableDeliveries.visibility = View.GONE
        binding.cardActiveDelivery.visibility = View.VISIBLE

        binding.activeOrderId.text = "#BT-${doc.id.take(6).uppercase()}"
        binding.activeClientName.text = doc.getString("userName") ?: "Cliente HiBeauty"
        binding.activeClientAddress.text = doc.getString("address") ?: "Dirección de entrega"
        
        val phone = doc.getString("userPhone") ?: ""
        binding.activeClientPhone.text = "Teléfono: $phone"

        // Summarize items
        val itemsList = doc.get("items") as? List<Map<*, *>>
        val itemsSummary = itemsList?.joinToString { item ->
            val qty = item["quantity"] ?: 1
            val name = item["name"] ?: "Producto"
            "${qty}x $name"
        } ?: "Productos de Belleza"
        binding.activeOrderItemsSummary.text = "Productos: $itemsSummary"

        val status = doc.getString("status")?.lowercase() ?: ""
        
        if (status == "en_camino") {
            binding.btnUpdateDeliveryStatus.text = "Marcar como Entregado"
            binding.btnUpdateDeliveryStatus.setOnClickListener {
                markAsDelivered(doc)
            }
        } else {
            binding.btnUpdateDeliveryStatus.text = "Marcar como En Camino"
            binding.btnUpdateDeliveryStatus.setOnClickListener {
                markAsOnTheWay(doc.id)
            }
        }

        // Call Button
        binding.btnCallClient.setOnClickListener {
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                startActivity(intent)
            } else {
                toast("El cliente no registró número telefónico")
            }
        }
    }

    private fun markAsOnTheWay(orderId: String) {
        val updates = hashMapOf(
            "status" to "En_camino",
            "statusLabel" to "Repartidor en camino a tu dirección",
            "statusUpdatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("orders").document(orderId).update(updates as Map<String, Any>)
            .addOnSuccessListener {
                toast("¡Vas en camino! Conduce con cuidado")
            }
            .addOnFailureListener {
                toast("Error al actualizar estado")
            }
    }

    private fun markAsDelivered(doc: DocumentSnapshot) {
        val riderUid = auth.currentUser?.uid ?: return
        val orderId = doc.id
        val shippingFee = doc.getLong("shipping") ?: 8000L

        val batch = db.batch()

        // 1. Update order status
        val orderRef = db.collection("orders").document(orderId)
        batch.update(orderRef, "status", "Entregado")
        batch.update(orderRef, "statusLabel", "Entregado con éxito")
        batch.update(orderRef, "statusUpdatedAt", FieldValue.serverTimestamp())

        // 2. Increment completed deliveries and add earnings to rider user document
        val riderRef = db.collection("users").document(riderUid)
        batch.update(riderRef, "completedDeliveries", FieldValue.increment(1))
        batch.update(riderRef, "earnings", FieldValue.increment(shippingFee))

        batch.commit()
            .addOnSuccessListener {
                toast("¡Pedido entregado con éxito! +${shippingFee.toCOP()} agregados a tus ganancias 💸")
            }
            .addOnFailureListener {
                toast("Error al finalizar entrega")
            }
    }

    private fun stopObservingDeliveries() {
        activeDeliveryListener?.remove()
        activeDeliveryListener = null
        availableOrdersListener?.remove()
        availableOrdersListener = null
    }

    private fun setupLogout() {
        binding.btnLogoutDelivery.setOnClickListener {
            auth.signOut()
            toast("Sesión cerrada")
            val mainActivity = requireActivity() as? MainActivity
            mainActivity?.showUserNavigation()
            mainActivity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_home
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopObservingDeliveries()
        riderStatsListener?.remove()
        _binding = null
    }
}
