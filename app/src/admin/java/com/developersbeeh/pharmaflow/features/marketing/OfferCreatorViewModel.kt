package com.developersbeeh.pharmaflow.features.marketing

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import com.developersbeeh.pharmaflow.utils.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfferCreatorViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Lista completa carregada da loja
    private var allProductsCache: List<Product> = emptyList()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _templateType = MutableStateFlow(0) // 0=Feed, 1=Story
    val templateType = _templateType.asStateFlow()

    // Estados de busca
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val currentStoreName = sessionManager.currentStore.value?.name ?: "Drogaria Santo Antônio"

    init {
        loadProducts()
    }

    private fun loadProducts() {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                val result = productRepository.getProductsByStore(storeId)
                val rawList = result.getOrDefault(emptyList())

                // FILTRO: Apenas produtos com foto para não gerar oferta feia
                allProductsCache = rawList.filter { it.imageUrl.isNotEmpty() }

                // Aplica filtro inicial (tudo)
                onSearchQueryChanged("")
            }
        }
    }

    // Busca inteligente "CONTENHA"
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _products.value = allProductsCache
        } else {
            // Busca permissiva (contém, sem case sensitive)
            _products.value = allProductsCache.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.ean.contains(query) ||
                        it.internalCode.contains(query)
            }
        }
    }

    // Função para buscar especificamente por código (usada pelo scanner)
    fun onBarcodeScanned(code: String) {
        _searchQuery.value = code
        // Tenta achar exato primeiro
        val match = allProductsCache.find { it.ean == code || it.internalCode == code }
        if (match != null) {
            selectProduct(match)
            _products.value = listOf(match)
        } else {
            // Se não achar exato, filtra
            onSearchQueryChanged(code)
        }
    }

    fun selectProduct(product: Product) { _selectedProduct.value = product }
    fun setTemplate(type: Int) { _templateType.value = type }

    fun shareOffer(context: Context, bitmap: Bitmap) {
        val product = _selectedProduct.value ?: return
        val text = "🔥 Oferta na $currentStoreName!\n${product.name}\nPor R$ ${String.format("%.2f", product.getCurrentPrice())}"
        ShareUtils.shareBitmap(context, bitmap, text)
    }
}