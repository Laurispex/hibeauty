package com.example.hibeauty

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentAdminPublishBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AdminPublishFragment : Fragment(R.layout.fragment_admin_publish) {

    private var _binding: FragmentAdminPublishBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        _binding = FragmentAdminPublishBinding.bind(view)

        binding.btnPublishProduct.setOnClickListener {
            publishProduct()
        }
    }

    private fun publishProduct() {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            toast("Inicia sesión como administrador")

            return
        }

        val name =
            binding.productName.text.toString().trim()

        val description =
            binding.productDescription.text.toString().trim()

        val imageUrl =
            binding.productImageUrl.text.toString().trim()

        val benefits =
            binding.productBenefits.text.toString().trim()

        val howToUse =
            binding.productHowToUse.text.toString().trim()

        val category =
            getSelectedCategory()

        val oldPrice =
            binding.productOldPrice.text
                .toString()
                .trim()
                .toLongOrNull() ?: 0L

        val isFeatured =
            binding.checkFeatured.isChecked

        val isNew =
            binding.checkNew.isChecked

        val isOffer =
            binding.checkOffer.isChecked

        // PRESENTACIONES

        val price30 =
            binding.price30.text.toString()
                .trim()
                .toLongOrNull()

        val stock30 =
            binding.stock30.text.toString()
                .trim()
                .toLongOrNull()

        val price50 =
            binding.price50.text.toString()
                .trim()
                .toLongOrNull()

        val stock50 =
            binding.stock50.text.toString()
                .trim()
                .toLongOrNull()

        val price100 =
            binding.price100.text.toString()
                .trim()
                .toLongOrNull()

        val stock100 =
            binding.stock100.text.toString()
                .trim()
                .toLongOrNull()

        // VALIDACIONES

        if (
            name.isEmpty() ||
            description.isEmpty() ||
            imageUrl.isEmpty() ||
            benefits.isEmpty() ||
            howToUse.isEmpty()
        ) {

            toast("Completa toda la información")

            return
        }

        if (!imageUrl.startsWith("http")) {

            toast("La URL debe iniciar con http")

            return
        }

        if (
            price30 == null || stock30 == null ||
            price50 == null || stock50 == null ||
            price100 == null || stock100 == null
        ) {

            toast("Completa todas las presentaciones")

            return
        }

        binding.btnPublishProduct.isEnabled = false

        binding.btnPublishProduct.text =
            "Publicando..."

        val productId =
            db.collection("products")
                .document()
                .id

        // PRESENTACIONES

        val presentations = hashMapOf(

            "30ml" to hashMapOf(
                "price" to price30,
                "stock" to stock30
            ),

            "50ml" to hashMapOf(
                "price" to price50,
                "stock" to stock50
            ),

            "100ml" to hashMapOf(
                "price" to price100,
                "stock" to stock100
            )
        )

        // PRODUCTO

        val product = hashMapOf(

            "id" to productId,

            "name" to name,

            "description" to description,

            "imageUrl" to imageUrl,

            "category" to category,

            "benefits" to benefits,

            "howToUse" to howToUse,

            "presentations" to presentations,

            "oldPrice" to oldPrice,

            "isFeatured" to isFeatured,

            "isNew" to isNew,

            "isOffer" to isOffer,

            "isActive" to true,

            "createdBy" to currentUser.uid,

            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("products")
            .document(productId)
            .set(product)

            .addOnSuccessListener {

                if (_binding == null)
                    return@addOnSuccessListener

                showPublishButtonAgain()

                clearForm()

                toast("Producto publicado ✨")
            }

            .addOnFailureListener { error ->

                if (_binding == null)
                    return@addOnFailureListener

                showPublishButtonAgain()

                toast(
                    error.message
                        ?: "Error publicando producto"
                )
            }
    }

    private fun getSelectedCategory(): String {

        return when (
            binding.productCategoryGroup.checkedRadioButtonId
        ) {

            R.id.categoryMakeup ->
                "Maquillaje"

            R.id.categoryFragrance ->
                "Fragancias"

            R.id.categoryWellness ->
                "Bienestar"

            else ->
                "Skincare"
        }
    }

    private fun clearForm() {

        binding.productName.text?.clear()

        binding.productDescription.text?.clear()

        binding.productImageUrl.text?.clear()

        binding.productBenefits.text?.clear()

        binding.productHowToUse.text?.clear()

        binding.productOldPrice.text?.clear()

        binding.price30.text?.clear()

        binding.stock30.text?.clear()

        binding.price50.text?.clear()

        binding.stock50.text?.clear()

        binding.price100.text?.clear()

        binding.stock100.text?.clear()

        binding.checkFeatured.isChecked = false

        binding.checkNew.isChecked = false

        binding.checkOffer.isChecked = false

        binding.categorySkincare.isChecked = true
    }

    private fun showPublishButtonAgain() {

        binding.btnPublishProduct.isEnabled = true

        binding.btnPublishProduct.text =
            "Publicar producto"
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