package com.developersbeeh.pharmaflow.features.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriceCheckViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _foundProduct = MutableStateFlow<Product?>(null)
    val foundProduct = _foundProduct.asStateFlow()

    private val _notFoundError = MutableStateFlow(false)
    val notFoundError = _notFoundError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Usado tanto pela Câmera quanto pela Digitação Manual
    fun onBarcodeScanned(rawCode: String) {
        // Evita processar se já está carregando ou se já tem um produto na tela (para não piscar)
        if (_isLoading.value || (_foundProduct.value != null && _foundProduct.value!!.ean == rawCode)) return

        val storeId = sessionManager.getCurrentStoreId()
        val code = rawCode.trim()

        if (storeId.isNotEmpty() && code.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true
                _notFoundError.value = false // Reseta erro anterior

                // Busca no servidor
                val result = repository.getProductByEan(storeId, code)
                val product = result.getOrNull()

                if (product != null) {
                    _foundProduct.value = product
                    _notFoundError.value = false
                } else {
                    triggerNotFoundError()
                }

                _isLoading.value = false
            }
        }
    }

    private fun triggerNotFoundError() {
        viewModelScope.launch {
            _notFoundError.value = true
            // Mostra o erro por 3 segundos e reseta para permitir tentar de novo
            delay(5000)
            _notFoundError.value = false
        }
    }

    fun clearResult() {
        _foundProduct.value = null
        _notFoundError.value = false
        _isLoading.value = false
    }
}