package com.example.hibeauty

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hibeauty.databinding.FragmentCategoryProductsBinding
import com.google.firebase.firestore.FirebaseFirestore

class CategoryProductsFragment(
    private val category: String
) : Fragment(R.layout.fragment_category_products) {

    private var _binding: FragmentCategoryProductsBinding? = null
    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val productAdapter = ProductAdapter { product ->
        (activity as? MainActivity)?.openProductDetail(product)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        _binding = FragmentCategoryProductsBinding.bind(view)

        setupUI()

        setupTabs()

        loadAllProducts()
    }

    private fun setupUI() {

        binding.categoryTitle.text =
            "$category ${getCategoryEmoji()}"

        binding.bannerTitle.text =
            "$category para ti"

        binding.bannerEmoji.text =
            getCategoryEmoji()

        binding.bannerSubtitle.text =
            getBannerSubtitle()

        binding.categoryRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 2)

        binding.categoryRecyclerView.adapter =
            productAdapter

        if (binding.categoryRecyclerView.itemDecorationCount == 0) {

            binding.categoryRecyclerView.addItemDecoration(
                GridSpacingItemDecoration(2, 24, true)
            )
        }

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    // TABS

    private fun setupTabs() {

        binding.tabAll.setOnClickListener {
            loadAllProducts()
        }

        binding.tabFeatured.setOnClickListener {
            loadFeaturedProducts()
        }

        binding.tabNew.setOnClickListener {
            loadNewProducts()
        }

        binding.tabOffers.setOnClickListener {
            loadOfferProducts()
        }
    }

    // TODOS

    private fun loadAllProducts() {

        db.collection("products")
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                updateProducts(products)
            }
    }

    // BESTSELLERS

    private fun loadFeaturedProducts() {

        db.collection("products")
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", category)
            .whereEqualTo("isFeatured", true)
            .get()
            .addOnSuccessListener { result ->

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                updateProducts(products)
            }
    }

    // NUEVOS

    private fun loadNewProducts() {

        db.collection("products")
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", category)
            .whereEqualTo("isNew", true)
            .get()
            .addOnSuccessListener { result ->

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                updateProducts(products)
            }
    }

    // OFERTAS

    private fun loadOfferProducts() {

        db.collection("products")
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", category)
            .whereEqualTo("isOffer", true)
            .get()
            .addOnSuccessListener { result ->

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                updateProducts(products)
            }
    }

    // UPDATE UI

    private fun updateProducts(products: List<Product>) {

        productAdapter.submitList(products)

        val countText =
            "${products.size} productos"

        binding.categoryCount.text =
            countText

        binding.productsCountText.text =
            countText
    }

    // EMOJIS

    private fun getCategoryEmoji(): String {

        return when (category) {

            "Skincare" -> "✨"

            "Maquillaje" -> "💄"

            "Fragancias" -> "🌸"

            "Bienestar" -> "🧘"

            else -> "💖"
        }
    }

    // SUBTITLES

    private fun getBannerSubtitle(): String {

        return when (category) {

            "Skincare" ->
                "Productos seleccionados con amor para tu piel"

            "Maquillaje" ->
                "Brilla con los tonos perfectos para ti"

            "Fragancias" ->
                "Aromas únicos que enamoran"

            "Bienestar" ->
                "Momentos de paz y autocuidado"

            else ->
                "Descubre productos increíbles"
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}