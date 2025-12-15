package com.developersbeeh.pharmaflow.features.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.model.User
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import com.developersbeeh.pharmaflow.features.auth.AuthRepository
import com.developersbeeh.pharmaflow.utils.GuarujaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- IMPORTANTE: Esta classe deve estar fora da classe DeliveryViewModel ---
sealed class DeliveryUiState {
    object Loading : DeliveryUiState()
    object NoZone : DeliveryUiState()
    data class Success(
        val tasks: List<Order>, 
        val activeDriver: User,
        val currentZone: String = "",
        val monitoredStores: List<String> = emptyList()
    ) : DeliveryUiState()
    data class Error(val msg: String) : DeliveryUiState()
}

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeliveryUiState>(DeliveryUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadDeliveryFeed()
    }

    private fun loadDeliveryFeed() {
        viewModelScope.launch {
            _uiState.value = DeliveryUiState.Loading

            val userResult = authRepository.getCurrentUser()
            val user = userResult.getOrNull()

            if (user != null) {
                // Verifica se tem zona definida
                if (user.driverZone.isNotEmpty()) {
                    val storesToMonitor = GuarujaUtils.getStoresByZone(user.driverZone)

                    if (storesToMonitor.isNotEmpty()) {
                        // O repository vai retornar o flow
                        orderRepository.getOrdersForDeliveryZone(storesToMonitor).collect { orders ->
                            // Filtra apenas o necessário na UI (apenas Delivery e ativos)
                            val tasks = orders.filter { it.isDelivery && it.status != "Cancelado" }
                            _uiState.value = DeliveryUiState.Success(
                                tasks = tasks, 
                                activeDriver = user,
                                currentZone = user.driverZone,
                                monitoredStores = storesToMonitor
                            )
                        }
                    } else {
                        _uiState.value = DeliveryUiState.Error("Nenhuma loja mapeada para zona: ${user.driverZone}")
                    }
                } else {
                    _uiState.value = DeliveryUiState.NoZone
                }
            } else {
                _uiState.value = DeliveryUiState.Error("Usuário não logado.")
            }
        }
    }

    fun acceptDelivery(order: Order) {
        if (order.status == "Em Separação") {
            viewModelScope.launch {
                val user = authRepository.getCurrentUser().getOrNull()
                val driverName = user?.name ?: "Motoboy (Desconhecido)"
                // A classe User usa 'id' como identificador, não 'uid'
                val driverId = user?.id ?: ""

                // Muda para "Saiu para Entrega" e ASSINA o pedido
                orderRepository.updateOrderStatus(
                    storeId = order.storeId, 
                    orderId = order.firestoreId, 
                    newStatus = "Saiu para Entrega",
                    driverId = driverId,
                    driverName = driverName
                )
            }
        }
    }

    fun completeDelivery(order: Order) {
        viewModelScope.launch {
            // Finaliza
            orderRepository.updateOrderStatus(order.storeId, order.firestoreId, "Entregue")
        }
    }
}