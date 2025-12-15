package com.developersbeeh.pharmaflow.features.delivery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class DeliveryDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _order = MutableStateFlow<Order?>(null)
    val order = _order.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Carrega o pedido usando o ID passado pela navegação
    fun loadOrder(orderId: String) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true
                val result = orderRepository.getOrderById(storeId, orderId)
                if (result.isSuccess) {
                    _order.value = result.getOrNull()
                }
                _isLoading.value = false
            }
        }
    }

    // Abre o app de mapas (Google Maps ou Waze) com o endereço
    fun openMaps(address: String) {
        try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val uri = Uri.parse("geo:0,0?q=$encodedAddress")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps") // Tenta forçar Google Maps primeiro
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback genérico se não tiver Maps
                val genericIntent = Intent(Intent.ACTION_VIEW, uri)
                genericIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(genericIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir mapa.", Toast.LENGTH_SHORT).show()
        }
    }

    // Lógica de transição de status para o Motoboy
    fun advanceStatus() {
        val currentOrder = _order.value ?: return
        val storeId = sessionManager.getCurrentStoreId()

        val nextStatus = when (currentOrder.status) {
            "Em Separação" -> "Saiu para Entrega"
            "Saiu para Entrega" -> "Entregue"
            else -> return
        }

        viewModelScope.launch {
            _isLoading.value = true
            orderRepository.updateOrderStatus(storeId, currentOrder.firestoreId, nextStatus)

            // Atualiza o estado local para refletir na UI imediatamente
            _order.value = currentOrder.copy(status = nextStatus)
            _isLoading.value = false

            Toast.makeText(context, "Status atualizado para: $nextStatus", Toast.LENGTH_SHORT).show()
        }
    }
}