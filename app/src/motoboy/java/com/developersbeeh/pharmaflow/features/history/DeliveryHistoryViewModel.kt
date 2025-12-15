package com.developersbeeh.pharmaflow.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _historyOrders = MutableStateFlow<List<Order>>(emptyList())
    val historyOrders = _historyOrders.asStateFlow()

    private val _totalDeliveryFees = MutableStateFlow(0.0)
    val totalDeliveryFees = _totalDeliveryFees.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true
                orderRepository.getOrdersByStoreFlow(storeId).collect { allOrders ->
                    // Filtra apenas entregues
                    val delivered = allOrders.filter { it.status == "Entregue" }
                    _historyOrders.value = delivered

                    // Calcula comissão (Soma das taxas de entrega)
                    _totalDeliveryFees.value = delivered.sumOf { it.deliveryFee }

                    _isLoading.value = false
                }
            }
        }
    }
}