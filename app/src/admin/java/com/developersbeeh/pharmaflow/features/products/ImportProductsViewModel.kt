package com.developersbeeh.pharmaflow.features.products

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import com.developersbeeh.pharmaflow.utils.CsvDataImporter
import com.developersbeeh.pharmaflow.utils.LegacyJsonImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportProductsViewModel @Inject constructor(
    application: Application,
    private val repository: ProductRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Progresso (0.0 a 1.0)
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress = _uploadProgress.asStateFlow()

    // Texto de Status (Ex: "500 / 92000")
    private val _progressText = MutableStateFlow("")
    val progressText = _progressText.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    fun importFile(uri: Uri) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isEmpty()) {
            _statusMessage.value = "Erro: Nenhuma loja selecionada."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _uploadProgress.value = 0f
            _statusMessage.value = "Lendo arquivo gigante (Aguarde)..."

            try {
                val context = getApplication<Application>().applicationContext
                val path = uri.path?.lowercase() ?: ""
                val isJson = path.endsWith(".json") || context.contentResolver.getType(uri)?.contains("json") == true

                // Leitura (pode demorar um pouco para 92k itens)
                val products = if (isJson) {
                    LegacyJsonImporter.parseLegacyJson(context, uri, storeId)
                } else {
                    CsvDataImporter.parseCsv(context, uri, storeId)
                }

                if (products.isNotEmpty()) {
                    _statusMessage.value = "Iniciando upload de ${products.size} itens..."

                    // Passa o callback de progresso
                    val result = repository.saveProductsBatch(products) { saved, total ->
                        _uploadProgress.value = saved.toFloat() / total.toFloat()
                        _progressText.value = "$saved de $total salvos"
                    }

                    if (result.isSuccess) {
                        val count = result.getOrDefault(0)
                        _statusMessage.value = "Sucesso Total! $count produtos importados."
                        _progressText.value = "Concluído"
                        _uploadProgress.value = 1f
                    } else {
                        _statusMessage.value = "Erro no meio do caminho: ${result.exceptionOrNull()?.message}"
                    }
                } else {
                    _statusMessage.value = "Arquivo vazio ou formato inválido."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erro crítico: ${e.message}"
                e.printStackTrace()
            }

            _isLoading.value = false
        }
    }
}