package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentAdminOrdersBinding
import com.example.hibeauty.ui.store.orders.StoreOrdersUiState
import com.example.hibeauty.ui.store.orders.StoreOrdersViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AdminOrdersFragment : Fragment() {

    private var _binding: FragmentAdminOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StoreOrdersViewModel by viewModels()

    private val orderAdapter = OrderAdapter(showStatusAction = true) { order ->
        viewModel.advanceStatus(order)
    }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adminOrdersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminOrdersRecyclerView.adapter = orderAdapter
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

    // ─── OBSERVERS ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StoreOrdersUiState.Loading -> {
                            binding.adminOrdersEmptyText.isVisible = true
                            binding.adminOrdersEmptyText.text = "Cargando pedidos..."
                            binding.adminOrdersRecyclerView.isVisible = false
                        }
                        is StoreOrdersUiState.Empty -> {
                            binding.adminOrdersEmptyText.isVisible = true
                            binding.adminOrdersEmptyText.text = "Todavía no hay pedidos."
                            binding.adminOrdersRecyclerView.isVisible = false
                        }
                        is StoreOrdersUiState.Ready -> {
                            orderAdapter.submitList(state.orders)
                            binding.adminOrdersEmptyText.isVisible = false
                            binding.adminOrdersRecyclerView.isVisible = true
                        }
                        is StoreOrdersUiState.Error -> {
                            binding.adminOrdersEmptyText.isVisible = true
                            binding.adminOrdersEmptyText.text = state.message
                            binding.adminOrdersRecyclerView.isVisible = false
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionResult.collect { msg ->
                    if (msg != null) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        viewModel.clearActionResult()
                    }
                }
            }
        }
    }
}
