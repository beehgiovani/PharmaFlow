package com.developersbeeh.pharmaflow.features.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct = _scannedProduct.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Flag para mostrar erro rápido
    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    fun onBarcodeScanned(code: String) {
        // Se já está processando ou já tem um produto na tela, ignora leituras rápidas da câmera
        if (_scannedProduct.value != null || _isLoading.value) return

        findProduct(code)
    }

    // Mesma função usada pelo botão manual
    fun findProduct(code: String) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMsg.value = null

                val result = repository.getProductByEan(storeId, code)
                val product = result.getOrNull()

                if (product != null) {
                    _scannedProduct.value = product
                } else {
                    _errorMsg.value = "Produto não encontrado: $code"
                    // Reseta msg após 3 segundos
                    kotlinx.coroutines.delay(3000)
                    _errorMsg.value = null
                }
                _isLoading.value = false
            }
        }
    }

    fun dismissProduct() {
        _scannedProduct.value = null
    }

    fun updateStock(newQuantity: Int) {
        val product = _scannedProduct.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateStock(product.id, newQuantity)

            if (result.isSuccess) {
                _scannedProduct.value = null // Fecha o card para o próximo item
            } else {
                _errorMsg.value = "Erro ao atualizar estoque."
            }
            _isLoading.value = false
        }
    }
}