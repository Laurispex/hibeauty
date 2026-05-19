package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentCartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val cartAdapter = CartAdapter { item ->
        removeCartItem(item)
    }

    private var cartItems: List<CartItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cartRecyclerView.adapter = cartAdapter

        binding.btnCheckout.setOnClickListener {
            checkout()
        }

        loadCart()
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }

    private fun loadCart() {
        val user = auth.currentUser

        if (user == null) {
            binding.cartEmptyText.isVisible = true
            binding.cartEmptyText.text = "Inicia sesión para ver tu carrito."
            binding.cartRecyclerView.isVisible = false
            binding.cartSubtotal.text = 0.toCOP()
            binding.cartShipping.text = 0.toCOP()
            binding.cartTotal.text = 0.toCOP()
            binding.btnCheckout.isEnabled = false
            return
        }

        binding.cartEmptyText.isVisible = true
        binding.cartEmptyText.text = "Cargando carrito..."
        binding.cartRecyclerView.isVisible = false
        binding.btnCheckout.isEnabled = false

        db.collection("carts")
            .document(user.uid)
            .collection("items")
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                cartItems = result.documents.map { document ->
                    CartItem(
                        id = document.id,
                        productId = document.getString("productId") ?: "",
                        name = document.getString("name") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        presentation = document.getString("presentation") ?: "",
                        price = document.getLong("price") ?: 0L,
                        quantity = document.getLong("quantity") ?: 1L
                    )
                }

                if (cartItems.isEmpty()) {
                    binding.cartEmptyText.isVisible = true
                    binding.cartEmptyText.text = "Tu carrito está vacío."
                    binding.cartRecyclerView.isVisible = false
                    binding.cartSubtotal.text = 0.toCOP()
                    binding.cartShipping.text = 0.toCOP()
                    binding.cartTotal.text = 0.toCOP()
                    binding.btnCheckout.isEnabled = false
                    return@addOnSuccessListener
                }

                cartAdapter.submitList(cartItems)
                binding.cartEmptyText.isVisible = false
                binding.cartRecyclerView.isVisible = true
                binding.btnCheckout.isEnabled = true
                updateTotal()
            }
            .addOnFailureListener {
                _binding?.cartEmptyText?.isVisible = true
                _binding?.cartEmptyText?.text = "No se pudo cargar el carrito."
                _binding?.cartRecyclerView?.isVisible = false
                _binding?.btnCheckout?.isEnabled = false
            }
    }

    private fun updateTotal() {
        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val shipping = if (cartItems.isEmpty()) 0L else 8000L
        val total = subtotal + shipping
        binding.cartSubtotal.text = subtotal.toCOP()
        binding.cartShipping.text = shipping.toCOP()
        binding.cartTotal.text = total.toCOP()
    }

    private fun removeCartItem(item: CartItem) {
        val user = auth.currentUser ?: return

        db.collection("carts")
            .document(user.uid)
            .collection("items")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                toast("Producto eliminado")
                loadCart()
            }
            .addOnFailureListener {
                toast("No se pudo eliminar")
            }
    }

    private fun checkout() {
        val user = auth.currentUser

        // NOT LOGGED

        if (user == null) {

            androidx.appcompat.app.AlertDialog
                .Builder(requireContext())

                .setTitle("Upss! 💕")

                .setMessage(
                    "Todas queremos ser BeautyLovers ✨\n\nPara continuar con tu compra regístrate e inicia sesión."
                )

                .setPositiveButton(
                    "Iniciar sesión"
                ) { _, _ ->

                    parentFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fragment_container,
                            ProfileFragment()
                        )
                        .addToBackStack(null)
                        .commit()
                }

                .setNegativeButton(
                    "Después",
                    null
                )

                .show()

            return
        }

        // EMPTY CART

        if (cartItems.isEmpty()) {

            toast("Tu carrito está vacío")

            return
        }

        // VALIDATION

        binding.btnCheckout.isEnabled = false

        binding.btnCheckout.text =
            "Validando..."

        validateStockAndCreateOrder(user.uid)
    }

    private fun validateStockAndCreateOrder(userId: String) {
        val productRefs = cartItems.map { item ->
            item to db.collection("products").document(item.productId)
        }

        val batch = db.batch()
        val checkedItems = mutableListOf<CartItem>()
        var pendingChecks = productRefs.size
        var hasError = false

        productRefs.forEach { pair ->
            val item = pair.first
            val productRef = pair.second

            productRef.get()
                .addOnSuccessListener { document ->
                    if (hasError) return@addOnSuccessListener

                    val presentations = document.get("presentations") as? Map<*, *>
                    val presentationData = presentations?.get(item.presentation) as? Map<*, *>
                    val currentStock = (presentationData?.get("stock") as? Number)?.toLong() ?: 0L

                    if (currentStock < item.quantity) {
                        hasError = true
                        showCheckoutButtonAgain()
                        toast("No hay stock suficiente de ${item.name} (${item.presentation})")
                        return@addOnSuccessListener
                    }

                    val newStock = currentStock - item.quantity

                    batch.update(
                        productRef,
                        "presentations.${item.presentation}.stock",
                        newStock
                    )

                    checkedItems.add(item)
                    pendingChecks--

                    if (pendingChecks == 0 && !hasError) {
                        createOrderAndCommitBatch(userId, batch, checkedItems)
                    }
                }
                .addOnFailureListener {
                    if (!hasError) {
                        hasError = true
                        showCheckoutButtonAgain()
                        toast("Error validando stock")
                    }
                }
        }
    }

    private fun createOrderAndCommitBatch(
        userId: String,
        batch: com.google.firebase.firestore.WriteBatch,
        checkedItems: List<CartItem>
    ) {
        binding.btnCheckout.text = "Creando pedido..."

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name") ?: "Cliente"
                val userPhone = userDoc.getString("phone") ?: "Sin teléfono"
                val address = userDoc.getString("address") ?: "Calle 85 # 11-53, Bogotá"

                val subtotal = checkedItems.sumOf { it.price * it.quantity }
                val shipping = 8000L
                val total = subtotal + shipping
                val earnedPoints = (subtotal / 1000L).coerceAtLeast(1L)
                val orderRef = db.collection("orders").document()

                val items = checkedItems.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "name" to item.name,
                        "imageUrl" to item.imageUrl,
                        "presentation" to item.presentation,
                        "price" to item.price,
                        "quantity" to item.quantity
                    )
                }

                val order = hashMapOf(
                    "id" to orderRef.id,
                    "userId" to userId,
                    "userName" to userName,
                    "userPhone" to userPhone,
                    "address" to address,
                    "status" to "Pendiente",
                    "statusLabel" to "Pedido recibido",
                    "subtotal" to subtotal,
                    "shipping" to shipping,
                    "total" to total,
                    "earnedPoints" to earnedPoints,
                    "items" to items,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "statusUpdatedAt" to FieldValue.serverTimestamp(),
                    "createdAtMillis" to System.currentTimeMillis(),
                    "statusHistory" to listOf(
                        hashMapOf(
                            "status" to "Pendiente",
                            "label" to "Pedido recibido",
                            "changedAtMillis" to System.currentTimeMillis()
                        )
                    )
                )

                batch.set(orderRef, order)
                batch.set(
                    db.collection("users").document(userId),
                    mapOf(
                        "points" to FieldValue.increment(earnedPoints),
                        "orderCount" to FieldValue.increment(1),
                        "totalSpent" to FieldValue.increment(total)
                    ),
                    SetOptions.merge()
                )

                checkedItems.forEach { item ->
                    val cartRef = db.collection("carts")
                        .document(userId)
                        .collection("items")
                        .document(item.id)

                    batch.delete(cartRef)
                }

                batch.commit()
                    .addOnSuccessListener {
                        showCheckoutButtonAgain()
                        toast("Pedido creado")
                        loadCart()
                    }
                    .addOnFailureListener {
                        showCheckoutButtonAgain()
                        toast("No se pudo crear el pedido")
                    }
            }
            .addOnFailureListener {
                showCheckoutButtonAgain()
                toast("Error al obtener datos del usuario")
            }
    }

    private fun showCheckoutButtonAgain() {
        binding.btnCheckout.isEnabled = true
        binding.btnCheckout.text = "Comprar"
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
