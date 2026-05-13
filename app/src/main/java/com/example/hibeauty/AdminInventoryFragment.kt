package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentAdminInventoryBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminInventoryFragment : Fragment() {

    private var _binding: FragmentAdminInventoryBinding? = null
    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val productAdapter = AdminProductAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adminInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminInventoryRecyclerView.adapter = productAdapter
        loadInventory()
    }

    override fun onResume() {
        super.onResume()
        loadInventory()
    }

    private fun loadInventory() {
        binding.adminInventoryText.isVisible = true
        binding.adminInventoryText.text = "Cargando inventario..."
        binding.adminInventoryRecyclerView.isVisible = false

        db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.map { document ->
                    document.toProduct()
                }

                if (products.isEmpty()) {
                    binding.adminInventoryText.isVisible = true
                    binding.adminInventoryText.text = "Todavia no hay productos publicados."
                    binding.adminInventoryRecyclerView.isVisible = false
                    return@addOnSuccessListener
                }

                productAdapter.submitList(products)
                binding.adminInventoryText.isVisible = false
                binding.adminInventoryRecyclerView.isVisible = true
            }
            .addOnFailureListener {
                loadInventoryWithoutOrder()
            }
    }

    private fun loadInventoryWithoutOrder() {
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.map { document ->
                    document.toProduct()
                }

                if (products.isEmpty()) {
                    binding.adminInventoryText.isVisible = true
                    binding.adminInventoryText.text = "Todavia no hay productos publicados."
                    binding.adminInventoryRecyclerView.isVisible = false
                    return@addOnSuccessListener
                }

                productAdapter.submitList(products)
                binding.adminInventoryText.isVisible = false
                binding.adminInventoryRecyclerView.isVisible = true
            }
            .addOnFailureListener {
                binding.adminInventoryText.isVisible = true
                binding.adminInventoryText.text = "No se pudo cargar el inventario."
                binding.adminInventoryRecyclerView.isVisible = false
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
