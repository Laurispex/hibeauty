package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hibeauty.databinding.FragmentCategoryProductsBinding
import com.example.hibeauty.ui.category.CategoryProductsViewModel
import kotlinx.coroutines.launch

class CategoryProductsFragment(
    private val category: String
) : Fragment(R.layout.fragment_category_products) {

    private var _binding: FragmentCategoryProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryProductsViewModel by viewModels()

    private val productAdapter = ProductAdapter { product ->
        (activity as? MainActivity)?.openProductDetail(product)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoryProductsBinding.bind(view)

        setupUI()
        setupTabs()
        observeViewModel()

        viewModel.loadAllProducts(category)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { products ->
                    productAdapter.submitList(products)
                    val countText = "${products.size} productos"
                    binding.categoryCount.text = countText
                    binding.productsCountText.text = countText
                }
            }
        }
    }

    private fun setupUI() {
        binding.categoryTitle.text = category
        binding.bannerTitle.text = "$category para ti"

        val iconRes = when (category) {
            "Skincare" -> R.drawable.ic_flower
            "Maquillaje" -> R.drawable.ic_lipstick
            "Fragancias" -> R.drawable.ic_lotion_bottle
            "Bienestar" -> R.drawable.ic_leaf
            else -> R.drawable.ic_crown
        }
        binding.bannerIcon.setImageResource(iconRes)
        binding.bannerSubtitle.text = getBannerSubtitle()

        binding.categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.categoryRecyclerView.adapter = productAdapter

        if (binding.categoryRecyclerView.itemDecorationCount == 0) {
            binding.categoryRecyclerView.addItemDecoration(GridSpacingItemDecoration(2, 24, true))
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupTabs() {
        binding.tabAll.setOnClickListener { viewModel.loadAllProducts(category) }
        binding.tabFeatured.setOnClickListener { viewModel.loadFeaturedProducts(category) }
        binding.tabNew.setOnClickListener { viewModel.loadNewProducts(category) }
        binding.tabOffers.setOnClickListener { viewModel.loadOfferProducts(category) }
    }

    private fun getBannerSubtitle(): String = when (category) {
        "Skincare" -> "Productos seleccionados con amor para tu piel"
        "Maquillaje" -> "Brilla con los tonos perfectos para ti"
        "Fragancias" -> "Aromas únicos que enamoran"
        "Bienestar" -> "Momentos de paz y autocuidado"
        else -> "Descubre productos increíbles"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
