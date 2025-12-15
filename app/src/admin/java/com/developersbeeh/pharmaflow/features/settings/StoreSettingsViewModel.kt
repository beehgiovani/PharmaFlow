package com.developersbeeh.pharmaflow.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Store
import com.developersbeeh.pharmaflow.data.repository.StoreRepository
import com.developersbeeh.pharmaflow.utils.ProductBatchEnricher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreSettingsViewModel @Inject constructor(
    private val repository: StoreRepository,
    private val sessionManager: SessionManager,
    private val productEnricher: ProductBatchEnricher
) : ViewModel() {

    private val _uiState = MutableStateFlow(Store())
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isEnriching = MutableStateFlow(false)
    val isEnriching = _isEnriching.asStateFlow()

    private val _enrichProgress = MutableStateFlow("Aguardando...")
    val enrichProgress = _enrichProgress.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val currentStore = sessionManager.currentStore.value
        if (currentStore != null) {
            _uiState.value = currentStore
        }
    }

    fun startEnrichment() {
        runBatchProcess(checkExisting = false)
    }

    fun startDeepRepair() {
        runBatchProcess(checkExisting = true)
    }

    private fun runBatchProcess(checkExisting: Boolean) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isEnriching.value = true
            productEnricher.enrichProducts(storeId, checkExistingImages = checkExisting) { msg ->
                _enrichProgress.value = msg
            }
            _isEnriching.value = false
        }
    }

    fun saveSettings(
        name: String,
        whatsapp: String,
        address: String,
        pixKeyRaw: String,
        pixKeyType: String,
        deliveryFeeStr: String,
        logoUri: Uri?,
        openingTime: String,
        closingTime: String,
        deliveryStart: String,
        deliveryEnd: String,
        storeActive: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val cleanWhatsapp = whatsapp.filter { it.isDigit() }
            val fee = deliveryFeeStr.replace(",", ".").toDoubleOrNull() ?: 0.0

            if (_uiState.value.id.isEmpty()) {
                _isLoading.value = false
                onResult(false)
                return@launch
            }

            var finalLogoUrl = _uiState.value.logoUrl
            if (logoUri != null) {
                val uploadResult = repository.uploadStoreLogo(logoUri)
                if (uploadResult.isSuccess) {
                    finalLogoUrl = uploadResult.getOrDefault("")
                }
            }

            // --- LÓGICA DE CORREÇÃO PIX (FORÇA +55) ---
            val finalPixKey = when (pixKeyType) {
                "Celular" -> {
                    // Limpa apenas números
                    val nums = pixKeyRaw.filter { it.isDigit() }

                    // Lógica blindada: Sempre inicia com +55 para celular BR
                    if (nums.startsWith("55") && nums.length > 11) {
                        // Caso usuário tenha digitado o 55 manualmente
                        "+$nums"
                    } else {
                        // Adiciona o prefixo Brasil
                        "+55$nums"
                    }
                }
                "E-mail" -> pixKeyRaw.trim().lowercase()
                "CPF", "CNPJ" -> pixKeyRaw.filter { it.isDigit() } // Remove pontos e traços
                else -> pixKeyRaw.trim() // Aleatória mantém
            }

            val updatedStore = _uiState.value.copy(
                name = name,
                whatsappNumber = cleanWhatsapp,
                address = address,
                pixKey = finalPixKey, // Chave já salva no formato correto (+55...)
                deliveryFee = fee,
                logoUrl = finalLogoUrl,
                openingTime = openingTime,
                closingTime = closingTime,
                deliveryStart = deliveryStart,
                deliveryEnd = deliveryEnd,
                active = storeActive
            )

            val result = repository.updateStore(updatedStore)

            if (result.isSuccess) {
                _uiState.value = updatedStore
                sessionManager.setStore(updatedStore)
            }

            _isLoading.value = false
            onResult(result.isSuccess)
        }
    }
}