package com.example.hibeauty

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hibeauty.databinding.FragmentSearchBinding
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment :
    Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val allProducts =
        mutableListOf<Product>()

    private val productAdapter =
        ProductAdapter { product ->

            (activity as? MainActivity)
                ?.openProductDetail(product)
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        _binding =
            FragmentSearchBinding.bind(view)

        setupRecycler()

        setupSearch()

        loadProducts()

        binding.btnBackSearch
            .setOnClickListener {

                parentFragmentManager.popBackStack()
            }
    }

    private fun setupRecycler() {

        binding.searchRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 2)

        binding.searchRecyclerView.adapter =
            productAdapter

        if (
            binding.searchRecyclerView
                .itemDecorationCount == 0
        ) {

            binding.searchRecyclerView
                .addItemDecoration(
                    GridSpacingItemDecoration(
                        2,
                        24,
                        true
                    )
                )
        }
    }

    private fun setupSearch() {

        binding.searchInput
            .addTextChangedListener(

                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        filterProducts(
                            s.toString()
                        )
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {
                    }
                }
            )
    }

    private fun loadProducts() {

        db.collection("products")
            .whereEqualTo("isActive", true)
            .get()

            .addOnSuccessListener { result ->

                allProducts.clear()

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                allProducts.addAll(products)

                productAdapter.submitList(products)

                updateResultsText(products.size)
            }
    }

    private fun filterProducts(query: String) {

        if (query.isBlank()) {

            productAdapter.submitList(allProducts)

            updateResultsText(
                allProducts.size
            )

            return
        }

        val filtered =
            allProducts.filter { product ->

                product.name.contains(
                    query,
                    ignoreCase = true
                )

                        ||

                        product.description.contains(
                            query,
                            ignoreCase = true
                        )

                        ||

                        product.category.contains(
                            query,
                            ignoreCase = true
                        )

                        ||

                        product.benefits.contains(
                            query,
                            ignoreCase = true
                        )
            }

        productAdapter.submitList(filtered)

        updateResultsText(filtered.size)
    }

    private fun updateResultsText(
        count: Int
    ) {

        binding.searchResultsText.text =
            "$count productos encontrados"
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}