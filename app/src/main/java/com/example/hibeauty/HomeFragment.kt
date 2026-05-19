package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hibeauty.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // PRODUCT ADAPTER

    private val productAdapter =
        ProductAdapter { product ->

            (activity as? MainActivity)
                ?.openProductDetail(product)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHomeBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        setupRecyclerView()

        setupQuickActions()

        setupCategories()

        setupSearch()

        loadUserGreeting()

        loadHomeProducts()

        setupProfileHeader()
    }

    private fun setupProfileHeader() {
        binding.btnProfileHeader.setOnClickListener {
            (activity as? MainActivity)?.openProfile()
        }
    }

    private fun loadUserGreeting() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name")
                    val greeting = if (!name.isNullOrEmpty()) {
                        "Hola, $name"
                    } else {
                        "Descubre tu rutina ideal"
                    }
                    _binding?.welcomeText?.text = greeting
                }
                .addOnFailureListener {
                    _binding?.welcomeText?.text = "Descubre tu rutina ideal"
                }
        } else {
            _binding?.welcomeText?.text = "Descubre tu rutina ideal"
        }
    }

    // RECYCLER VIEW

    private fun setupRecyclerView() {

        binding.homeProductsRecyclerView
            .layoutManager =
            GridLayoutManager(
                requireContext(),
                2
            )

        binding.homeProductsRecyclerView
            .adapter =
            productAdapter

        if (
            binding.homeProductsRecyclerView
                .itemDecorationCount == 0
        ) {

            binding.homeProductsRecyclerView
                .addItemDecoration(
                    GridSpacingItemDecoration(
                        2,
                        24,
                        true
                    )
                )
        }
    }

    // QUICK ACTIONS

    private fun setupQuickActions() {

        binding.quickRoutineCard.root
            .setOnClickListener {

                (activity as? MainActivity)
                    ?.openRoutine()
            }

        binding.quickOrdersCard.root
            .setOnClickListener {

                (activity as? MainActivity)
                    ?.openProfile()
            }

        // OPEN GUIDE

        binding.btnOpenGuide
            .setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        SkinCareGuideFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }

    // SEARCH

    private fun setupSearch() {

        binding.searchBar
            .setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        SearchFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }

    // CATEGORIES

    private fun setupCategories() {

        binding.categorySkincare.root
            .setOnClickListener {

                openCategory("Skincare")
            }

        binding.categoryMakeup.root
            .setOnClickListener {

                openCategory("Maquillaje")
            }

        binding.categoryFragrance.root
            .setOnClickListener {

                openCategory("Fragancias")
            }

        binding.categoryWellness.root
            .setOnClickListener {

                openCategory("Bienestar")
            }
    }

    // OPEN CATEGORY

    private fun openCategory(
        category: String
    ) {

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_container,
                CategoryProductsFragment(category)
            )
            .addToBackStack(null)
            .commit()
    }

    // LOAD PRODUCTS

    private fun loadHomeProducts() {

        val currentBinding =
            _binding ?: return

        currentBinding.homeProductsEmptyText
            .isVisible = true

        currentBinding.homeProductsEmptyText.text =
            "Cargando productos..."

        currentBinding.homeProductsRecyclerView
            .isVisible = false

        db.collection("products")
            .whereEqualTo(
                "isActive",
                true
            )
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .get()

            .addOnSuccessListener { result ->

                val binding =
                    _binding
                        ?: return@addOnSuccessListener

                if (result.isEmpty) {

                    binding.homeProductsTitle.text =
                        "Volvieron tus favoritos 💖"

                    binding.homeProductsEmptyText.text =
                        "Aún no hay productos publicados."

                    binding.homeProductsRecyclerView
                        .isVisible = false

                    return@addOnSuccessListener
                }

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                binding.homeProductsTitle.text =
                    "Productos de la semana ✨"

                productAdapter.submitList(
                    products
                )

                binding.homeProductsEmptyText
                    .isVisible = false

                binding.homeProductsRecyclerView
                    .isVisible = true
            }

            .addOnFailureListener {

                loadHomeProductsWithoutOrder()
            }
    }

    // FALLBACK

    private fun loadHomeProductsWithoutOrder() {

        db.collection("products")
            .whereEqualTo(
                "isActive",
                true
            )
            .get()

            .addOnSuccessListener { result ->

                val binding =
                    _binding
                        ?: return@addOnSuccessListener

                if (result.isEmpty) {

                    binding.homeProductsTitle.text =
                        "Volvieron tus favoritos 💖"

                    binding.homeProductsEmptyText
                        .isVisible = true

                    binding.homeProductsEmptyText.text =
                        "Aún no hay productos publicados."

                    binding.homeProductsRecyclerView
                        .isVisible = false

                    return@addOnSuccessListener
                }

                val products =
                    result.documents.map {
                        it.toProduct()
                    }

                binding.homeProductsTitle.text =
                    "Productos de la semana ✨"

                productAdapter.submitList(
                    products
                )

                binding.homeProductsEmptyText
                    .isVisible = false

                binding.homeProductsRecyclerView
                    .isVisible = true
            }

            .addOnFailureListener {

                val binding =
                    _binding
                        ?: return@addOnFailureListener

                binding.homeProductsEmptyText
                    .isVisible = true

                binding.homeProductsEmptyText.text =
                    "No se pudieron cargar los productos."

                binding.homeProductsRecyclerView
                    .isVisible = false
            }
    }

    override fun onResume() {

        super.onResume()

        loadHomeProducts()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}