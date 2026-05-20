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
import com.example.hibeauty.databinding.FragmentAdminInventoryBinding
import com.example.hibeauty.ui.store.inventory.InventoryUiState
import com.example.hibeauty.ui.store.inventory.StoreInventoryViewModel
import kotlinx.coroutines.launch

class AdminInventoryFragment : Fragment() {

    private var _binding: FragmentAdminInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StoreInventoryViewModel by viewModels()

    private val productAdapter = AdminProductAdapter { product ->
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AdminPublishFragment().apply {
                arguments = Bundle().apply { putString("product_id", product.id) }
            })
            .addToBackStack(null).commit()
    }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adminInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminInventoryRecyclerView.adapter = productAdapter
        binding.btnBackInventory.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnAddProduct.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminPublishFragment())
                .addToBackStack(null).commit()
        }
        observeViewModel()
        viewModel.load()
    }

    override fun onResume() { super.onResume(); viewModel.load() }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // ─── OBSERVER ──────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is InventoryUiState.Loading -> {
                            binding.adminInventoryText.isVisible = true
                            binding.adminInventoryText.text = "Cargando inventario..."
                            binding.adminInventoryRecyclerView.isVisible = false
                        }
                        is InventoryUiState.Empty -> {
                            binding.adminInventoryText.isVisible = true
                            binding.adminInventoryText.text = "Todavía no hay productos publicados."
                            binding.adminInventoryRecyclerView.isVisible = false
                        }
                        is InventoryUiState.Ready -> {
                            productAdapter.submitList(state.products)
                            binding.adminInventoryText.isVisible = false
                            binding.adminInventoryRecyclerView.isVisible = true
                        }
                        is InventoryUiState.Error -> {
                            binding.adminInventoryText.isVisible = true
                            binding.adminInventoryText.text = state.message
                            binding.adminInventoryRecyclerView.isVisible = false
                        }
                    }
                }
            }
        }
    }
}
