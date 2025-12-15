package com.developersbeeh.pharmaflow.features.products

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.remote.ProductSearchService
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import com.developersbeeh.pharmaflow.utils.ImageProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddProductViewModel @Inject constructor(
    application: Application,
    private val repository: ProductRepository,
    private val sessionManager: SessionManager,
    private val searchService: ProductSearchService
) : AndroidViewModel(application) {

    private val imageProcessor = ImageProcessor(application.applicationContext)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _productState = MutableStateFlow<Product?>(null)
    val productState = _productState.asStateFlow()

    // Imagem LOCAL (caso o usuário tire foto na hora)
    private val _processedImageFile = MutableStateFlow<File?>(null)
    val processedImageFile = _processedImageFile.asStateFlow()

    // Imagem REMOTA (vinda da API, para preencher a URL direto)
    private val _remoteImageUrl = MutableStateFlow<String?>(null)
    val remoteImageUrl = _remoteImageUrl.asStateFlow()

    private val _foundOnlineName = MutableStateFlow<String?>(null)
    val foundOnlineName = _foundOnlineName.asStateFlow()

    private val _foundDescription = MutableStateFlow<String?>(null)
    val foundDescription = _foundDescription.asStateFlow()

    fun loadProduct(productId: String) {
        if (productId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getProductById(productId)
            if (result.isSuccess) _productState.value = result.getOrNull()
            _isLoading.value = false
        }
    }

    // --- BUSCA ONLINE VIA EAN ---
    fun searchProductByEan(ean: String) {
        if (ean.length < 8) return

        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>().applicationContext
            Toast.makeText(context, "Buscando dados na nuvem...", Toast.LENGTH_SHORT).show()

            val result = searchService.searchByEan(ean)

            if (result.found) {
                // 1. Preenche dados
                if (result.name.isNotEmpty()) {
                    _foundOnlineName.value = result.name
                }
                if (result.description.isNotEmpty()) {
                    _foundDescription.value = result.description
                }

                // 2. Preenche Imagem
                if (result.imageUrl.isNotEmpty()) {
                    // Aqui definimos a URL remota diretamente.
                    // A tela de cadastro deve priorizar: processedImageFile > remoteImageUrl > currentImageUrl
                    _remoteImageUrl.value = result.imageUrl
                    Toast.makeText(context, "Dados e Imagem encontrados!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Dados encontrados, sem imagem.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Produto não encontrado nas APIs.", Toast.LENGTH_SHORT).show()
            }
            _isLoading.value = false
        }
    }

    // --- SELEÇÃO DA GALERIA ---
    fun onImageSelected(uri: Uri) {
        // ... (Mantém a lógica existente para processar imagem da galeria se o usuário quiser trocar)
    }

    fun consumeOnlineName() { _foundOnlineName.value = null }
    fun consumeDescription() { _foundDescription.value = null }

    fun saveProduct(
        id: String = "",
        name: String,
        ean: String,
        internalCode: String,
        priceStr: String,
        promoPriceStr: String,
        stockStr: String,
        description: String,
        category: String,
        currentImageUrl: String,
        expirationDate: Long = 0L,
        isFeatured: Boolean,
        isActive: Boolean = true,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val price = priceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val promoPrice = promoPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val stock = stockStr.toIntOrNull() ?: 0

            // LÓGICA DE PRIORIDADE DA IMAGEM:
            // 1. Novo Upload (processedImageFile) - O usuário tirou foto agora.
            // 2. URL Remota (remoteImageUrl) - Veio da API (eanpictures) e o usuário não trocou.
            // 3. Imagem Atual (currentImageUrl) - Já estava no produto editado.

            var finalImageUrl = currentImageUrl

            if (_processedImageFile.value != null) {
                // Faz upload apenas se houver um arquivo físico novo
                val uri = Uri.fromFile(_processedImageFile.value)
                val uploadResult = repository.uploadImage(uri)
                if (uploadResult.isSuccess) {
                    finalImageUrl = uploadResult.getOrDefault("")
                }
            } else if (_remoteImageUrl.value != null) {
                // Usa a URL externa direta (Sem custo de storage!)
                finalImageUrl = _remoteImageUrl.value!!
            }

            val currentStoreId = sessionManager.getCurrentStoreId()

            val product = Product(
                id = id,
                storeId = currentStoreId,
                name = name,
                ean = ean,
                internalCode = internalCode,
                price = price,
                promoPrice = promoPrice,
                description = description,
                category = category,
                imageUrl = finalImageUrl,
                stockQuantity = stock,
                expirationDate = expirationDate,
                isFeatured = isFeatured,
                isActive = isActive
            )

            val result = repository.saveProduct(product)
            _isLoading.value = false
            onResult(result.isSuccess)
        }
    }

    fun deleteProduct(productId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteProduct(productId)
            _isLoading.value = false
            onResult(result.isSuccess)
        }
    }
}