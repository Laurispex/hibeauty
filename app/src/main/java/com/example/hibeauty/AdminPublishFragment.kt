package com.example.hibeauty

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hibeauty.R
import com.example.hibeauty.databinding.FragmentAdminPublishBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AdminPublishFragment : Fragment(R.layout.fragment_admin_publish) {

    private var _binding: FragmentAdminPublishBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Dynamic list to store presentations configured by the administrator
    private val presentationList = mutableListOf<PresentationItem>()

    // Secure memory storage for the uploaded Cloudinary secure link
    private var uploadedImageUrl: String = ""

    // ==========================================
    // ☁️ CONFIGURACIÓN DE CLOUDINARY
    // ==========================================
    private val CLOUDINARY_CLOUD_NAME = "dswicy00p"     // Tu Cloud Name
    private val CLOUDINARY_UPLOAD_PRESET = "hi-beauty"   // Tu Upload Preset Unsigned

    // Activity Result Launcher to pick images from the device gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Immediately show the selected image in the preview card
            binding.imagePublishPreview.setImageURI(uri)
            // Trigger the direct upload to Cloudinary in the background
            uploadImageToCloudinary(uri)
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminPublishBinding.bind(view)

        // Setup Back Button
        binding.btnBackPublish.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup Direct Image Gallery Picker
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Setup Add Presentation Action
        binding.btnAddPresentation.setOnClickListener {
            addPresentationToList()
        }

        // Setup Publish Button
        binding.btnPublishProduct.setOnClickListener {
            publishProduct()
        }
    }

    private fun addPresentationToList() {
        val name = binding.inputPresetName.text.toString().trim()
        val priceText = binding.inputPresetPrice.text.toString().trim()
        val stockText = binding.inputPresetStock.text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || stockText.isEmpty()) {
            toast("Ingresa nombre, precio y stock de la presentación")
            return
        }

        val price = priceText.toLongOrNull()
        val stock = stockText.toLongOrNull()

        if (price == null || price <= 0) {
            toast("El precio debe ser un número válido mayor a 0")
            return
        }
        if (stock == null || stock < 0) {
            toast("El stock debe ser un número válido igual o mayor a 0")
            return
        }

        // Check if name is already added to avoid duplicate keys in Firestore map
        if (presentationList.any { it.name.lowercase() == name.lowercase() }) {
            toast("Ya has añadido una presentación con el nombre '$name'")
            return
        }

        val newItem = PresentationItem(name, price, stock)
        presentationList.add(newItem)

        // Dynamically inflate item_presentation_chip.xml in the layout container
        val inflater = LayoutInflater.from(requireContext())
        val chipView = inflater.inflate(R.layout.item_presentation_chip, binding.containerPresentations, false)

        chipView.findViewById<TextView>(R.id.txtPresName).text = newItem.name
        chipView.findViewById<TextView>(R.id.txtPresPrice).text = newItem.price.toCOP() // Leveraging our COP formatter!
        chipView.findViewById<TextView>(R.id.txtPresStock).text = "${newItem.stock} uds"

        // Handle delete presentation action
        chipView.findViewById<ImageView>(R.id.btnDeletePres).setOnClickListener {
            presentationList.remove(newItem)
            binding.containerPresentations.removeView(chipView)
            toast("Presentación eliminada")
        }

        binding.containerPresentations.addView(chipView)

        // Clear dynamic inputs to facilitate adding more presentations
        binding.inputPresetName.text?.clear()
        binding.inputPresetPrice.text?.clear()
        binding.inputPresetStock.text?.clear()
        binding.inputPresetName.requestFocus()
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        // Show loading layouts and disable interactions
        binding.uploadProgressLayout.visibility = View.VISIBLE
        binding.btnPublishProduct.isEnabled = false
        binding.btnSelectImage.isEnabled = false

        Thread {
            try {
                // 1. Read input stream and convert file to Base64
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                inputStream?.close()

                val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUrl = "data:image/jpeg;base64,$base64Image"

                // 2. Setup POST request to Cloudinary Unsigned Upload API
                val url = URL("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Parameters: file (Base64 dataUrl) and upload_preset
                val postData = "file=" + URLEncoder.encode(dataUrl, "UTF-8") +
                        "&upload_preset=" + URLEncoder.encode(CLOUDINARY_UPLOAD_PRESET, "UTF-8")

                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(Charsets.UTF_8))
                }

                // 3. Process the Response
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val secureUrl = json.getString("secure_url") // Retrieve the final Cloud HTTPS URL

                    activity?.runOnUiThread {
                        uploadedImageUrl = secureUrl
                        binding.uploadProgressLayout.visibility = View.GONE
                        binding.btnPublishProduct.isEnabled = true
                        binding.btnSelectImage.isEnabled = true
                        toast("¡Imagen subida a la nube con éxito! ☁️")
                        // Use Glide to render the final cloud image into the preview safely
                        loadImagePreview(secureUrl)
                    }
                } else {
                    val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error desconocido"
                    activity?.runOnUiThread {
                        binding.uploadProgressLayout.visibility = View.GONE
                        binding.btnPublishProduct.isEnabled = true
                        binding.btnSelectImage.isEnabled = true
                        toast("Error al subir a Cloudinary. Verifica Cloud Name y Preset.")
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.uploadProgressLayout.visibility = View.GONE
                    binding.btnPublishProduct.isEnabled = true
                    binding.btnSelectImage.isEnabled = true
                    toast("Fallo de red: ${e.message}")
                }
            }
        }.start()
    }

    private fun loadImagePreview(url: String) {
        if (url.isNotBlank()) {
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_lotion_bottle)
                .error(R.drawable.ic_warning)
                .centerCrop()
                .into(binding.imagePublishPreview)
        } else {
            binding.imagePublishPreview.setImageResource(R.drawable.ic_lotion_bottle)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    private fun publishProduct() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            toast("Inicia sesión como administrador")
            return
        }

        val name = binding.productName.text.toString().trim()
        val description = binding.productDescription.text.toString().trim()
        val imageUrl = uploadedImageUrl
        val benefits = binding.productBenefits.text.toString().trim()
        val howToUse = binding.productHowToUse.text.toString().trim()
        val category = getSelectedCategory()

        val isFeatured = binding.checkFeatured.isChecked
        val isNew = binding.checkNew.isChecked
        val isOffer = binding.checkOffer.isChecked

        if (name.isEmpty() || description.isEmpty() || imageUrl.isEmpty() || benefits.isEmpty() || howToUse.isEmpty()) {
            toast("Completa toda la información e incluye la foto")
            return
        }

        if (presentationList.isEmpty()) {
            toast("Debes añadir al menos una presentación (nombre, precio y stock)")
            return
        }

        binding.btnPublishProduct.isEnabled = false
        binding.btnPublishProduct.text = "Publicando..."

        val productId = db.collection("products").document().id

        // Build Firestore presentations map dynamically from the user-added dynamic list
        val presentationsMap = hashMapOf<String, Any>()
        for (item in presentationList) {
            presentationsMap[item.name] = hashMapOf(
                "price" to item.price,
                "stock" to item.stock
            )
        }

        val productMap = hashMapOf(
            "id" to productId,
            "name" to name,
            "description" to description,
            "imageUrl" to imageUrl,
            "category" to category,
            "benefits" to benefits,
            "howToUse" to howToUse,
            "presentations" to presentationsMap,
            "oldPrice" to 0L, // Fixed to default 0 since old price / offers are removed
            "isFeatured" to isFeatured,
            "isNew" to isNew,
            "isOffer" to isOffer,
            "isActive" to true,
            "createdBy" to currentUser.uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("products")
            .document(productId)
            .set(productMap)
            .addOnSuccessListener {
                toast("Producto publicado ✨")
                clearForm()
                binding.btnPublishProduct.isEnabled = true
                binding.btnPublishProduct.text = "Publicar producto"
            }
            .addOnFailureListener {
                toast("Error publicando producto")
                binding.btnPublishProduct.isEnabled = true
                binding.btnPublishProduct.text = "Publicar producto"
            }
    }

    private fun getSelectedCategory(): String {
        return when (binding.productCategoryGroup.checkedRadioButtonId) {
            R.id.categoryMakeup -> "Maquillaje"
            R.id.categoryFragrance -> "Fragancias"
            R.id.categoryWellness -> "Bienestar"
            else -> "Skincare"
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

        // Clear dynamic presentations list state and layout views
        presentationList.clear()
        binding.containerPresentations.removeAllViews()

        // Clear local memory image URL state
        uploadedImageUrl = ""

        loadImagePreview("")
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

// Simple data class representation for dynamic product presentations
data class PresentationItem(
    val name: String,
    val price: Long,
    val stock: Long
)