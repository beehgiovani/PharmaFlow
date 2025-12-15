package com.developersbeeh.pharmaflow.features.orders

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.model.Store
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import com.developersbeeh.pharmaflow.data.repository.StoreRepository
import com.developersbeeh.pharmaflow.features.auth.AuthRepository
import com.developersbeeh.pharmaflow.utils.PrinterHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminOrdersUiState(
    val isLoading: Boolean = true,
    val activeOrders: List<Order> = emptyList(),
    val historyOrders: List<Order> = emptyList(),
    val availableStores: List<Store> = emptyList(),
    val bluetoothDevices: List<BluetoothDevice> = emptyList(), // Lista de Impressoras
    val error: String? = null
)

@HiltViewModel
class AdminOrdersViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context // Necessário para Bluetooth
) : ViewModel() {

    private val _rawOrders = MutableStateFlow<List<Order>>(emptyList())
    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList()) // Estado local de devices
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AdminOrdersUiState> = combine(
        _rawOrders, _isLoading, _error, _availableStores, _bluetoothDevices
    ) { orders, loading, error, stores, devices ->
        val (active, history) = orders.partition {
            it.status != "Entregue" && it.status != "Cancelado"
        }
        AdminOrdersUiState(
            isLoading = loading,
            activeOrders = active,
            historyOrders = history,
            availableStores = stores,
            bluetoothDevices = devices,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminOrdersUiState())

    init {
        loadOrders()
        loadStoresForTransfer()
    }

    // --- FUNÇÕES DE PEDIDOS (MANTIDAS) ---
    private fun loadOrders() {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                repository.getOrdersByStoreFlow(storeId).collect { orders ->
                    _rawOrders.value = orders
                    _isLoading.value = false
                }
            }
        }
    }

    private fun loadStoresForTransfer() {
        viewModelScope.launch {
            val currentId = sessionManager.getCurrentStoreId()
            val result = storeRepository.getAllStores()
            if (result.isSuccess) {
                _availableStores.value = result.getOrDefault(emptyList()).filter { it.id != currentId }
            }
        }
    }

    fun transferOrder(order: Order, targetStore: Store) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.transferOrder(order, targetStore.id, targetStore.name)
            _isLoading.value = false
        }
    }

    fun advanceStatus(order: Order) {
        val nextStatus = when (order.status) {
            "Pendente" -> "Em Separação"
            "Em Separação" -> "Saiu para Entrega"
            "Saiu para Entrega" -> "Entregue"
            else -> return
        }
        updateStatus(order, nextStatus)
    }

    fun cancelOrder(order: Order, reason: String) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch { repository.cancelOrder(storeId, order.firestoreId, reason) }
        }
    }

    private fun updateStatus(order: Order, newStatus: String) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                repository.updateOrderStatus(storeId, order.firestoreId, newStatus)
                if (newStatus == "Entregue" && order.userId.isNotEmpty()) {
                    val points = order.totalAmount.toInt()
                    if (points > 0) authRepository.addPoints(order.userId, points)
                }
            }
        }
    }

    // --- FUNÇÕES DE IMPRESSÃO ---

    fun scanBluetoothDevices() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == true) {
                // Em produção, cheque permissões BLUETOOTH_CONNECT no Android 12+
                val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                _bluetoothDevices.value = pairedDevices.toList()
            } else {
                _error.value = "Bluetooth desligado."
            }
        } catch (e: SecurityException) {
            _error.value = "Sem permissão de Bluetooth."
        }
    }

    fun printOrder(order: Order, device: BluetoothDevice) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = PrinterHelper.printOrder(context, order, device)
            if (result.isFailure) {
                _error.value = "Erro na impressão: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }
}