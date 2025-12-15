package com.developersbeeh.pharmaflow.features.product_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.repository.CartRepository
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProductDetailUiState {
    object Loading : ProductDetailUiState()
    data class Success(val product: Product) : ProductDetailUiState()
    data class Error(val message: String) : ProductDetailUiState()
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Estado local para o seletor de quantidade (Padrão: 1)
    private val _quantity = MutableStateFlow(1)
    val quantity = _quantity.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            
            val result = productRepository.getProductById(productId)

            if (result.isSuccess) {
                val product = result.getOrNull()
                if (product != null) {
                    _uiState.value = ProductDetailUiState.Success(product)
                } else {
                    _uiState.value = ProductDetailUiState.Error("Produto não encontrado.")
                }
            } else {
                _uiState.value = ProductDetailUiState.Error("Erro de conexão ao carregar produto.")
            }
        }
    }

    fun incrementQuantity() {
        val current = _quantity.value
        // Limitamos a 99 por segurança de UX
        if (current < 99) {
            _quantity.value = current + 1
        }
    }

    fun decrementQuantity() {
        val current = _quantity.value
        if (current > 1) {
            _quantity.value = current - 1
        }
    }

    // Função aprimorada para adicionar N vezes
    fun addToCart(product: Product) {
        viewModelScope.launch {
            val qtyToAdd = _quantity.value

            // Loop para simular adição múltipla já que o repositório atual adiciona unitariamente.
            // *Nota de melhoria futura:* Criar função addWithQuantity(product, qty) no CartRepository
            repeat(qtyToAdd) {
                cartRepository.addToCart(product)
            }

            // Resetamos a quantidade para 1 após adicionar
            _quantity.value = 1
        }
    }
}