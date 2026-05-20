package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentUserOrdersBinding
import com.example.hibeauty.ui.orders.UserOrdersUiState
import com.example.hibeauty.ui.orders.UserOrdersViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class UserOrdersFragment : Fragment() {

    private var _binding: FragmentUserOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserOrdersViewModel by viewModels()

    private val orderAdapter = OrderAdapter(showStatusAction = false) { /* read-only */ }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.userOrdersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userOrdersRecyclerView.adapter = orderAdapter
        binding.btnBackOrders.setOnClickListener { parentFragmentManager.popBackStack() }
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

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // ─── OBSERVER ──────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UserOrdersUiState.Loading -> {
                            binding.userOrdersEmptyText.isVisible = true
                            binding.userOrdersEmptyText.text = "Cargando tus pedidos..."
                            binding.userOrdersRecyclerView.isVisible = false
                        }
                        is UserOrdersUiState.NotLoggedIn -> {
                            binding.userOrdersEmptyText.isVisible = true
                            binding.userOrdersEmptyText.text = "Inicia sesión para ver tus pedidos."
                            binding.userOrdersRecyclerView.isVisible = false
                        }
                        is UserOrdersUiState.Empty -> {
                            binding.userOrdersEmptyText.isVisible = true
                            binding.userOrdersEmptyText.text = "Aún no tienes pedidos. ¡Haz tu primera compra! 💖"
                            binding.userOrdersRecyclerView.isVisible = false
                        }
                        is UserOrdersUiState.Ready -> {
                            orderAdapter.submitList(state.orders)
                            binding.userOrdersEmptyText.isVisible = false
                            binding.userOrdersRecyclerView.isVisible = true
                        }
                        is UserOrdersUiState.Error -> {
                            binding.userOrdersEmptyText.isVisible = true
                            binding.userOrdersEmptyText.text = state.message
                            binding.userOrdersRecyclerView.isVisible = false
                        }
                    }
                }
            }
        }
    }
}
