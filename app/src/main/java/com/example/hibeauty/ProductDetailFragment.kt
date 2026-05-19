package com.example.hibeauty

import android.os.Bundle
import android.graphics.Paint
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hibeauty.databinding.FragmentProductDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment(
    private val product: Product
) : Fragment(R.layout.fragment_product_detail) {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var selectedSize: String = "30ml"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        _binding = FragmentProductDetailBinding.bind(view)

        setupProduct()

        setupPresentationSelector()

        binding.btnAddToCart.setOnClickListener {
            addToCart()
        }

        binding.btnBackDetail.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupProduct() {

        binding.detailName.text = product.name

        binding.detailDescription.text = product.description

        binding.detailCategory.text = product.category

        binding.detailHowToUse.text = product.howToUse

        binding.detailBenefits.text =
            product.benefits
                .split(",")
                .joinToString("\n") {
                    "• ${it.trim()}"
                }

        Glide.with(requireContext())
            .load(product.imageUrl)
            .centerCrop()
            .into(binding.detailImage)

        // BADGES DINÁMICOS

        when {

            product.isFeatured -> {
                binding.detailCategory.text = " Bestseller"
            }

            product.isNew -> {
                binding.detailCategory.text = "🆕 Nuevo"
            }

            product.isOffer -> {
                binding.detailCategory.text = "🏷 Oferta"
            }

            else -> {
                binding.detailCategory.text = product.category
            }
        }
    }

    private fun setupPresentationSelector() {

        binding.size30.isEnabled =
            product.presentations["30ml"] != null

        binding.size50.isEnabled =
            product.presentations["50ml"] != null

        binding.size100.isEnabled =
            product.presentations["100ml"] != null

        val firstAvailable =
            listOf("30ml", "50ml", "100ml")
                .firstOrNull { size ->

                    val presentation =
                        product.presentations[size]

                    presentation != null &&
                            presentation.stock > 0
                }

        if (firstAvailable == null) {

            binding.detailPrice.text = 0.toCOP()

            binding.detailStock.text =
                "Sin stock disponible"

            binding.btnAddToCart.isEnabled = false

            binding.btnAddToCart.text =
                "Sin stock"

            return
        }

        selectedSize = firstAvailable

        when (firstAvailable) {

            "30ml" -> binding.size30.isChecked = true

            "50ml" -> binding.size50.isChecked = true

            "100ml" -> binding.size100.isChecked = true
        }

        updatePresentationInfo(firstAvailable)

        binding.presentationGroup.setOnCheckedChangeListener { group, checkedId ->

            val selected =
                group.findViewById<RadioButton>(checkedId)
                    ?.text
                    ?.toString()
                    ?: return@setOnCheckedChangeListener

            selectedSize = selected

            updatePresentationInfo(selected)
        }
    }

    private fun updatePresentationInfo(size: String) {

        val presentation =
            product.presentations[size]

        if (presentation == null) {

            binding.detailPrice.text = 0.toCOP()

            binding.detailStock.text =
                "No disponible"

            binding.btnAddToCart.isEnabled = false

            binding.btnAddToCart.text =
                "No disponible"

            return
        }

        binding.detailPrice.text =
            presentation.price.toCOP()

        binding.detailStock.text =
            "Stock disponible: ${presentation.stock}"

        // PRECIO ANTERIOR SI ES OFERTA

        if (product.isOffer && product.oldPrice > 0) {

            binding.detailOldPrice.visibility = View.VISIBLE

            binding.detailOldPrice.text =
                product.oldPrice.toCOP()

            binding.detailOldPrice.paintFlags =
                binding.detailOldPrice.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG

        } else {

            binding.detailOldPrice.visibility = View.GONE
        }

        // STOCK

        if (presentation.stock <= 0) {

            binding.btnAddToCart.isEnabled = false

            binding.btnAddToCart.text =
                "Sin stock"

        } else {

            binding.btnAddToCart.isEnabled = true

            binding.btnAddToCart.text =
                "Comprar"
        }
    }

    private fun addToCart() {

        val user = auth.currentUser

        if (user == null) {

            toast("Inicia sesión para comprar")

            return
        }

        val presentation =
            product.presentations[selectedSize]

        if (presentation == null ||
            presentation.stock <= 0
        ) {

            toast("Esta presentación no tiene stock")

            return
        }

        val cartItem = hashMapOf(

            "productId" to product.id,

            "name" to product.name,

            "imageUrl" to product.imageUrl,

            "presentation" to selectedSize,

            "price" to presentation.price,

            "quantity" to 1,

            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("carts")
            .document(user.uid)
            .collection("items")
            .add(cartItem)
            .addOnSuccessListener {

                toast("Producto agregado al carrito 💖")
            }
            .addOnFailureListener {

                toast("No se pudo agregar al carrito")
            }
    }



    private fun toast(message: String) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}