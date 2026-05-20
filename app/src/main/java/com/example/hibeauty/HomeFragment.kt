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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hibeauty.databinding.FragmentHomeBinding
import com.example.hibeauty.ui.home.HomeUiState
import com.example.hibeauty.ui.home.HomeViewModel
import com.example.hibeauty.util.GridSpacingItemDecoration
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel survives rotation — no more data re-fetch on screen flip
    private val viewModel: HomeViewModel by viewModels()

    private val productAdapter = ProductAdapter { product ->
        (activity as? MainActivity)?.openProductDetail(product)
    }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── SETUP ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        binding.homeProductsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            if (itemDecorationCount == 0) addItemDecoration(GridSpacingItemDecoration(2, 24))
        }
    }

    private fun setupClickListeners() {
        binding.btnProfileHeader.setOnClickListener {
            (activity as? MainActivity)?.openProfile()
        }
        binding.quickRoutineCard.root.setOnClickListener {
            (activity as? MainActivity)?.openRoutine()
        }
        binding.quickOrdersCard.root.setOnClickListener {
            (activity as? MainActivity)?.openProfile()
        }
        binding.btnOpenGuide.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SkinCareGuideFragment())
                .addToBackStack(null).commit()
        }
        binding.searchBar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SearchFragment())
                .addToBackStack(null).commit()
        }
        binding.categorySkincare.root.setOnClickListener { openCategory("Skincare") }
        binding.categoryMakeup.root.setOnClickListener { openCategory("Maquillaje") }
        binding.categoryFragrance.root.setOnClickListener { openCategory("Fragancias") }
        binding.categoryWellness.root.setOnClickListener { openCategory("Bienestar") }
    }

    // ─── OBSERVER ──────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        // Update greeting from ViewModel (which reads from UserRepository)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Loading -> showLoading()
                        is HomeUiState.Ready   -> showProducts(state)
                        is HomeUiState.Error   -> showError(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.greeting.collect { greeting ->
                    binding.welcomeText.text = greeting
                }
            }
        }
    }

    // ─── UI STATES ─────────────────────────────────────────────────────────────

    private fun showLoading() {
        binding.homeProductsEmptyText.isVisible = true
        binding.homeProductsEmptyText.text = "Cargando productos..."
        binding.homeProductsRecyclerView.isVisible = false
    }

    private fun showProducts(state: HomeUiState.Ready) {
        val products = state.featured + state.newArrivals + state.offers
        if (products.isEmpty()) {
            binding.homeProductsTitle.text = "Volvieron tus favoritos 💖"
            binding.homeProductsEmptyText.isVisible = true
            binding.homeProductsEmptyText.text = "Aún no hay productos publicados."
            binding.homeProductsRecyclerView.isVisible = false
        } else {
            binding.homeProductsTitle.text = "Productos de la semana"
            productAdapter.submitList(products)
            binding.homeProductsEmptyText.isVisible = false
            binding.homeProductsRecyclerView.isVisible = true
        }
    }

    private fun showError(message: String) {
        binding.homeProductsEmptyText.isVisible = true
        binding.homeProductsEmptyText.text = message
        binding.homeProductsRecyclerView.isVisible = false
    }

    // ─── NAVIGATION ────────────────────────────────────────────────────────────

    private fun openCategory(category: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CategoryProductsFragment(category))
            .addToBackStack(null).commit()
    }
}
