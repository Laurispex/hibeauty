package com.example.hibeauty.ui.store.publish

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.repository.ImageRepository
import com.example.hibeauty.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PublishUiState {
    object Idle : PublishUiState()
    object Loading : PublishUiState()
    data class Success(val message: String) : PublishUiState()
    data class Error(val message: String) : PublishUiState()
    data class ImageUploading(val progress: String) : PublishUiState()
    data class ImageReady(val url: String) : PublishUiState()
}

class StorePublishViewModel(
    private val productRepo: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val uiState: StateFlow<PublishUiState> = _uiState

    private var imageRepo: ImageRepository? = null

    fun initImageRepo(context: Context) {
        if (imageRepo == null) imageRepo = ImageRepository(context.applicationContext)
    }

    fun uploadImage(uri: Uri) {
        val repo = imageRepo ?: return
        viewModelScope.launch {
            _uiState.value = PublishUiState.ImageUploading("Subiendo imagen...")
            repo.uploadImage(uri).fold(
                onSuccess = { url -> _uiState.value = PublishUiState.ImageReady(url) },
                onFailure = { _uiState.value = PublishUiState.Error("Error subiendo imagen: ${it.message}") }
            )
        }
    }

    fun saveProduct(data: Map<String, Any>, productId: String? = null) {
        viewModelScope.launch {
            _uiState.value = PublishUiState.Loading
            productRepo.saveProduct(data, productId).fold(
                onSuccess = {
                    _uiState.value = PublishUiState.Success(
                        if (productId != null) "Producto actualizado" else "Producto publicado"
                    )
                },
                onFailure = { _uiState.value = PublishUiState.Error("Error guardando: ${it.message}") }
            )
        }
    }

    fun loadProductForEdit(productId: String, onResult: (com.example.hibeauty.data.model.Product) -> Unit) {
        viewModelScope.launch {
            productRepo.getProductById(productId).onSuccess { onResult(it) }
        }
    }

    fun resetState() { _uiState.value = PublishUiState.Idle }
}
