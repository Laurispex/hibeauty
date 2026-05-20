package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentCartBinding
import com.example.hibeauty.ui.cart.CartUiState
import com.example.hibeauty.ui.cart.CartViewModel
import com.example.hibeauty.ui.cart.CheckoutState
import com.example.hibeauty.util.toCOP
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CartViewModel by viewModels()

    private val cartAdapter = CartAdapter { item ->
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@CartAdapter
        viewModel.removeItem(uid, item)
    }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cartRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cartRecyclerView.adapter = cartAdapter

        binding.btnCheckout.setOnClickListener { initiateCheckout() }
        binding.btnAddMore.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        binding.layoutStandardDelivery.setOnClickListener {
            binding.layoutStandardDelivery.setBackgroundResource(R.drawable.bg_status_track)
            binding.layoutExpressDelivery.setBackgroundResource(R.drawable.bg_search)
            viewModel.setShippingCost(8000L)
        }

        binding.layoutExpressDelivery.setOnClickListener {
            binding.layoutStandardDelivery.setBackgroundResource(R.drawable.bg_search)
            binding.layoutExpressDelivery.setBackgroundResource(R.drawable.bg_status_track)
            viewModel.setShippingCost(15000L)
        }

        observeViewModel()
        loadCart()
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── LOAD ──────────────────────────────────────────────────────────────────

    private fun loadCart() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        viewModel.loadCart(uid)
    }

    // ─── OBSERVERS ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartState.collect { state ->
                    when (state) {
                        is CartUiState.Loading    -> showLoading()
                        is CartUiState.NotLoggedIn -> showNotLoggedIn()
                        is CartUiState.Empty      -> showEmpty()
                        is CartUiState.Ready      -> showCart(state)
                        is CartUiState.Error      -> showError(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.checkoutState.collect { state ->
                    when (state) {
                        is CheckoutState.Idle    -> resetCheckoutButton()
                        is CheckoutState.Loading -> {
                            binding.btnCheckout.isEnabled = false
                            binding.btnCheckout.text = "Validando stock..."
                        }
                        is CheckoutState.Success -> {
                            resetCheckoutButton()
                            toast("Pedido creado correctamente 🎉")
                            viewModel.resetCheckout()
                            loadCart()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, UserOrdersFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                        is CheckoutState.StockError -> {
                            resetCheckoutButton()
                            toast(state.message)
                            viewModel.resetCheckout()
                        }
                        is CheckoutState.Error   -> {
                            resetCheckoutButton()
                            toast(state.message)
                            viewModel.resetCheckout()
                        }
                    }
                }
            }
        }
    }

    // ─── UI STATES ─────────────────────────────────────────────────────────────

    private fun showLoading() {
        binding.cartEmptyText.isVisible = true
        binding.cartEmptyText.text = "Cargando carrito..."
        binding.cartRecyclerView.isVisible = false
        binding.btnCheckout.isEnabled = false
        resetTotals()
    }

    private fun showNotLoggedIn() {
        binding.cartEmptyText.isVisible = true
        binding.cartEmptyText.text = "Inicia sesión para ver tu carrito."
        binding.cartRecyclerView.isVisible = false
        binding.btnCheckout.isEnabled = false
        resetTotals()
    }

    private fun showEmpty() {
        binding.cartEmptyText.isVisible = true
        binding.cartEmptyText.text = "Tu carrito está vacío."
        binding.cartRecyclerView.isVisible = false
        binding.btnCheckout.isEnabled = false
        resetTotals()
    }

    private fun showCart(state: CartUiState.Ready) {
        cartAdapter.submitList(state.items)
        binding.cartEmptyText.isVisible = false
        binding.cartRecyclerView.isVisible = true
        binding.btnCheckout.isEnabled = true
        binding.cartSubtotal.text = state.subtotal.toCOP()
        binding.cartShipping.text = state.shipping.toCOP()
        binding.cartTotal.text = state.total.toCOP()
    }

    private fun showError(message: String) {
        binding.cartEmptyText.isVisible = true
        binding.cartEmptyText.text = message
        binding.cartRecyclerView.isVisible = false
        binding.btnCheckout.isEnabled = false
    }

    private fun resetTotals() {
        binding.cartSubtotal.text = 0L.toCOP()
        binding.cartShipping.text = 0L.toCOP()
        binding.cartTotal.text = 0L.toCOP()
    }

    private fun resetCheckoutButton() {
        binding.btnCheckout.isEnabled = true
        binding.btnCheckout.text = "Pedir Contraentrega"
    }

    // ─── CHECKOUT ──────────────────────────────────────────────────────────────

    private fun initiateCheckout() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            showLoginDialog()
            return
        }
        val state = viewModel.cartState.value as? CartUiState.Ready ?: return
        showAddressDialog { address ->
            viewModel.checkout(user.uid, state.items, address)
        }
    }

    private fun showAddressDialog(onConfirm: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            hint = "Dirección de entrega"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
            setSingleLine(false)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Dirección de entrega")
            .setMessage("Confirma dónde quieres recibir tu pedido.")
            .setView(input)
            .setPositiveButton("Crear pedido") { _, _ ->
                val address = input.text.toString().trim()
                if (address.isBlank()) {
                    toast("Ingresa una dirección de entrega")
                } else {
                    onConfirm(address)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLoginDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Upss! 💕")
            .setMessage("Para continuar con tu compra, inicia sesión.")
            .setPositiveButton("Iniciar sesión") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .addToBackStack(null).commit()
            }
            .setNegativeButton("Después", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
}
