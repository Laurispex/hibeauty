package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.hibeauty.databinding.FragmentAdminPublishBinding
import com.example.hibeauty.ui.store.publish.PublishUiState
import com.example.hibeauty.ui.store.publish.StorePublishViewModel
import com.example.hibeauty.util.toCOP
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch

class AdminPublishFragment : Fragment(R.layout.fragment_admin_publish) {

    private var _binding: FragmentAdminPublishBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StorePublishViewModel by viewModels()

    // In-memory list of presentations configured by the admin
    private val presentationList = mutableListOf<PresentationItem>()

    // URL returned by Cloudinary via ViewModel
    private var uploadedImageUrl: String = ""

    private var productIdToEdit: String? = null

    // ─── IMAGE PICKER ──────────────────────────────────────────────────────────

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            binding.imagePublishPreview.setImageURI(uri)
            viewModel.uploadImage(uri)                // Coroutine in ViewModel, not Thread
        }
    }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminPublishBinding.bind(view)

        viewModel.initImageRepo(requireContext())

        productIdToEdit = arguments?.getString("product_id")
        if (productIdToEdit != null) {
            binding.btnPublishProduct.text = "Guardar cambios"
            loadProductForEdit(productIdToEdit!!)
        }

        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── OBSERVER ──────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PublishUiState.Idle -> Unit

                        is PublishUiState.ImageUploading -> {
                            binding.uploadProgressLayout.visibility = View.VISIBLE
                            binding.btnPublishProduct.isEnabled = false
                            binding.btnSelectImage.isEnabled = false
                        }

                        is PublishUiState.ImageReady -> {
                            uploadedImageUrl = state.url
                            binding.uploadProgressLayout.visibility = View.GONE
                            binding.btnPublishProduct.isEnabled = true
                            binding.btnSelectImage.isEnabled = true
                            loadImagePreview(state.url)
                            toast("¡Imagen subida a la nube! ☁️")
                            viewModel.resetState()
                        }

                        is PublishUiState.Loading -> {
                            binding.btnPublishProduct.isEnabled = false
                            binding.btnPublishProduct.text =
                                if (productIdToEdit != null) "Guardando..." else "Publicando..."
                        }

                        is PublishUiState.Success -> {
                            toast(state.message)
                            if (productIdToEdit != null) {
                                parentFragmentManager.popBackStack()
                            } else {
                                clearForm()
                                binding.btnPublishProduct.isEnabled = true
                                binding.btnPublishProduct.text = "Publicar producto"
                            }
                            viewModel.resetState()
                        }

                        is PublishUiState.Error -> {
                            binding.uploadProgressLayout.visibility = View.GONE
                            binding.btnPublishProduct.isEnabled = true
                            binding.btnSelectImage.isEnabled = true
                            binding.btnPublishProduct.text =
                                if (productIdToEdit != null) "Guardar cambios" else "Publicar producto"
                            toast(state.message)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    // ─── CLICK LISTENERS ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBackPublish.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnAddPresentation.setOnClickListener { addPresentationToList() }
        binding.btnPublishProduct.setOnClickListener { publishProduct() }
    }

    // ─── FORM LOGIC ────────────────────────────────────────────────────────────

    private fun addPresentationToList() {
        val name = binding.inputPresetName.text.toString().trim()
        val priceText = binding.inputPresetPrice.text.toString().trim()
        val stockText = binding.inputPresetStock.text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || stockText.isEmpty()) {
            toast("Ingresa nombre, precio y stock de la presentación"); return
        }
        val price = priceText.toLongOrNull()
        val stock = stockText.toLongOrNull()
        if (price == null || price <= 0) { toast("El precio debe ser mayor a 0"); return }
        if (stock == null || stock < 0) { toast("El stock debe ser 0 o mayor"); return }
        if (presentationList.any { it.name.lowercase() == name.lowercase() }) {
            toast("Ya existe una presentación con el nombre '$name'"); return
        }

        val newItem = PresentationItem(name, price, stock)
        presentationList.add(newItem)

        val chipView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_presentation_chip, binding.containerPresentations, false)
        chipView.findViewById<TextView>(R.id.txtPresName).text = newItem.name
        chipView.findViewById<TextView>(R.id.txtPresPrice).text = newItem.price.toCOP()
        chipView.findViewById<TextView>(R.id.txtPresStock).text = "${newItem.stock} uds"
        chipView.findViewById<ImageView>(R.id.btnDeletePres).setOnClickListener {
            presentationList.remove(newItem)
            binding.containerPresentations.removeView(chipView)
            toast("Presentación eliminada")
        }
        binding.containerPresentations.addView(chipView)

        binding.inputPresetName.text?.clear()
        binding.inputPresetPrice.text?.clear()
        binding.inputPresetStock.text?.clear()
        binding.inputPresetName.requestFocus()
    }

    private fun publishProduct() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { toast("Inicia sesión como tienda"); return }

        val name = binding.productName.text.toString().trim()
        val description = binding.productDescription.text.toString().trim()
        val benefits = binding.productBenefits.text.toString().trim()
        val howToUse = binding.productHowToUse.text.toString().trim()
        val category = getSelectedCategory()
        val isFeatured = binding.checkFeatured.isChecked
        val isNew = binding.checkNew.isChecked
        val isOffer = binding.checkOffer.isChecked

        if (name.isEmpty() || description.isEmpty() || uploadedImageUrl.isEmpty()
            || benefits.isEmpty() || howToUse.isEmpty()
        ) {
            toast("Completa toda la información e incluye la foto"); return
        }
        if (presentationList.isEmpty()) {
            toast("Añade al menos una presentación (nombre, precio y stock)"); return
        }

        val presentationsMap = hashMapOf<String, Any>()
        for (item in presentationList) {
            presentationsMap[item.name] = hashMapOf("price" to item.price, "stock" to item.stock)
        }

        val productData = hashMapOf<String, Any>(
            "name" to name,
            "description" to description,
            "imageUrl" to uploadedImageUrl,
            "category" to category,
            "benefits" to benefits,
            "howToUse" to howToUse,
            "presentations" to presentationsMap,
            "oldPrice" to 0L,
            "isFeatured" to isFeatured,
            "isNew" to isNew,
            "isOffer" to isOffer,
            "isActive" to true,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        viewModel.saveProduct(productData, productIdToEdit)
    }

    // ─── EDIT MODE ─────────────────────────────────────────────────────────────

    private fun loadProductForEdit(productId: String) {
        viewModel.loadProductForEdit(productId) { product ->
            val b = _binding ?: return@loadProductForEdit
            b.productName.setText(product.name)
            b.productDescription.setText(product.description)
            b.productBenefits.setText(product.benefits)
            b.productHowToUse.setText(product.howToUse)
            b.checkFeatured.isChecked = product.isFeatured
            b.checkNew.isChecked = product.isNew
            b.checkOffer.isChecked = product.isOffer

            when (product.category) {
                "Maquillaje" -> b.productCategoryGroup.check(R.id.categoryMakeup)
                "Fragancias" -> b.productCategoryGroup.check(R.id.categoryFragrance)
                "Bienestar"  -> b.productCategoryGroup.check(R.id.categoryWellness)
                else         -> b.productCategoryGroup.check(R.id.categorySkincare)
            }

            uploadedImageUrl = product.imageUrl
            loadImagePreview(product.imageUrl)

            presentationList.clear()
            b.containerPresentations.removeAllViews()
            product.presentations.forEach { (name, pres) ->
                val item = PresentationItem(name, pres.price, pres.stock)
                presentationList.add(item)

                val chipView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_presentation_chip, b.containerPresentations, false)
                chipView.findViewById<TextView>(R.id.txtPresName).text = item.name
                chipView.findViewById<TextView>(R.id.txtPresPrice).text = item.price.toCOP()
                chipView.findViewById<TextView>(R.id.txtPresStock).text = "${item.stock} uds"
                chipView.findViewById<ImageView>(R.id.btnDeletePres).setOnClickListener {
                    presentationList.remove(item)
                    b.containerPresentations.removeView(chipView)
                    toast("Presentación eliminada")
                }
                b.containerPresentations.addView(chipView)
            }
        }
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private fun getSelectedCategory() = when (binding.productCategoryGroup.checkedRadioButtonId) {
        R.id.categoryMakeup    -> "Maquillaje"
        R.id.categoryFragrance -> "Fragancias"
        R.id.categoryWellness  -> "Bienestar"
        else                   -> "Skincare"
    }

    private fun loadImagePreview(url: String) {
        if (url.isNotBlank()) {
            Glide.with(this).load(url)
                .placeholder(R.drawable.ic_lotion_bottle)
                .error(R.drawable.ic_warning)
                .centerCrop()
                .into(binding.imagePublishPreview)
        } else {
            binding.imagePublishPreview.setImageResource(R.drawable.ic_lotion_bottle)
        }
    }

    private fun clearForm() {
        binding.productName.text?.clear()
        binding.productDescription.text?.clear()
        binding.productBenefits.text?.clear()
        binding.productHowToUse.text?.clear()
        binding.inputPresetName.text?.clear()
        binding.inputPresetPrice.text?.clear()
        binding.inputPresetStock.text?.clear()
        binding.checkFeatured.isChecked = false
        binding.checkNew.isChecked = false
        binding.checkOffer.isChecked = false
        presentationList.clear()
        binding.containerPresentations.removeAllViews()
        uploadedImageUrl = ""
        loadImagePreview("")
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
}

/** Local data class for presentation rows. */
data class PresentationItem(val name: String, val price: Long, val stock: Long)
