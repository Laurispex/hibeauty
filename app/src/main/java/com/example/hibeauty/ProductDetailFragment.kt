package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.hibeauty.databinding.FragmentProductDetailBinding
import com.example.hibeauty.ui.product.ProductDetailViewModel
import kotlinx.coroutines.launch

class ProductDetailFragment(
    private val product: com.example.hibeauty.data.model.Product
) : Fragment(R.layout.fragment_product_detail) {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels()

    private var selectedPresentation: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductDetailBinding.bind(view)

        setupProduct()
        setupPresentationSelector()
        observeViewModel()

        binding.btnAddToCart.setOnClickListener {
            viewModel.addToCart(product, selectedPresentation)
        }

        binding.btnBackDetail.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionResult.collect { result ->
                    if (result != null) {
                        toast(result)
                        viewModel.clearResult()
                    }
                }
            }
        }
    }

    private fun setupProduct() {
        binding.detailName.text = product.name
        binding.detailDescription.text = product.description
        binding.detailCategory.text = product.category
        binding.detailHowToUse.text = product.howToUse
        binding.detailBenefits.text = product.benefits.split(",").joinToString("\n") { "• ${it.trim()}" }

        Glide.with(requireContext()).load(product.imageUrl).centerCrop().into(binding.detailImage)

        when {
            product.isFeatured -> binding.detailCategory.text = " Bestseller"
            product.isNew -> binding.detailCategory.text = "🆕 Nuevo"
            product.isOffer -> binding.detailCategory.text = "🏷 Oferta"
            else -> binding.detailCategory.text = product.category
        }
    }

    private fun setupPresentationSelector() {
        binding.presentationGroup.removeAllViews()

        if (product.presentations.isEmpty()) {
            binding.detailPrice.text = 0.toCOP()
            binding.detailStock.text = "Sin presentaciones disponibles"
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "No disponible"
            return
        }

        val buttonIdsByPresentation = mutableMapOf<Int, String>()
        val sortedPresentations = product.presentations.entries.sortedBy { it.key.lowercase() }

        sortedPresentations.forEach { (name, presentation) ->
            val button = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = "$name - ${presentation.price.toCOP()}"
                isEnabled = presentation.stock > 0
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
            }
            buttonIdsByPresentation[button.id] = name
            binding.presentationGroup.addView(button)
        }

        val firstAvailable = sortedPresentations.firstOrNull { it.value.stock > 0 }
        if (firstAvailable == null) {
            selectedPresentation = sortedPresentations.first().key
            updatePresentationInfo(selectedPresentation)
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "Sin stock"
            return
        }

        selectedPresentation = firstAvailable.key
        val checkedButtonId = buttonIdsByPresentation.entries
            .firstOrNull { it.value == selectedPresentation }
            ?.key
        if (checkedButtonId != null) {
            binding.presentationGroup.check(checkedButtonId)
        }

        updatePresentationInfo(selectedPresentation)

        binding.presentationGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = buttonIdsByPresentation[checkedId] ?: return@setOnCheckedChangeListener
            selectedPresentation = selected
            updatePresentationInfo(selectedPresentation)
        }
    }

    private fun updatePresentationInfo(size: String) {
        val presentation = product.presentations[size]

        if (presentation == null) {
            binding.detailPrice.text = 0.toCOP()
            binding.detailStock.text = "No disponible"
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "No disponible"
            return
        }

        binding.detailPrice.text = presentation.price.toCOP()
        binding.detailStock.text = "Stock disponible: ${presentation.stock}"

        if (product.isOffer && product.oldPrice > 0) {
            binding.detailOldPrice.visibility = View.VISIBLE
            binding.detailOldPrice.text = product.oldPrice.toCOP()
            binding.detailOldPrice.paintFlags = binding.detailOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.detailOldPrice.visibility = View.GONE
        }

        if (presentation.stock <= 0) {
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "Sin stock"
        } else {
            binding.btnAddToCart.isEnabled = true
            binding.btnAddToCart.text = "Comprar"
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
