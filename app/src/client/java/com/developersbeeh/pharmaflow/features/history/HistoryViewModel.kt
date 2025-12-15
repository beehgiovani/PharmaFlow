package com.developersbeeh.pharmaflow.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import com.developersbeeh.pharmaflow.features.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            viewModelScope.launch {
                // Escuta o Firestore em tempo real
                orderRepository.getClientOrdersRealtime(userId).collect { remoteOrders ->
                    _orders.value = remoteOrders
                    _isLoading.value = false
                }
            }
        } else {
            _isLoading.value = false
        }
    }
}