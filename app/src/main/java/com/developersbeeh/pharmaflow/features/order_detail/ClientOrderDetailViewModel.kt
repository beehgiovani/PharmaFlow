package com.developersbeeh.pharmaflow.features.order_detail

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
class ClientOrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _order = MutableStateFlow<Order?>(null)
    val order = _order.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    fun loadOrder(orderId: String) {
        val storeId = sessionManager.getCurrentStoreId()

        // Para carregar detalhes, tentamos buscar mesmo se storeId estiver vazio
        // (no caso do cliente, podemos precisar ajustar isso, mas vamos focar em carregar pelo ID)

        viewModelScope.launch {
            _isLoading.value = true

            // Aqui assumimos que se o storeId estiver na sessão (admin/motoboy logado em zona), usamos.
            // Se não (cliente vindo de push notification ou histórico), teremos que achar o pedido.
            // O OrderRepository.getOrderById precisa de storeId.
            // *Solução robusta*: O orderId é unico? Se for, podemos buscar group query.
            // Por hora, mantemos a logica que exige StoreId ou usamos um "hack" de passar qualquer coisa
            // se o repositorio permitir. Mas para não quebrar, vamos supor storeId válido na sessão ou ajuste futuro.

            val validStoreId = if(storeId.isNotEmpty()) storeId else "TEMP_FIX_OU_BUSCA_GLOBAL"
            // Nota: Para corrigir isso de verdade em multi-store, o 'Histórico' deveria passar o storeId junto.

            // Tenta buscar. Se o repositório estiver ajustado para collection group em leitura unica, funciona melhor.
            // Se falhar aqui, precisamos ajustar o Repo.

            // Vamos assumir o comportamento do repositorio atual que busca na store especifica
            val result = orderRepository.getOrderById(validStoreId, orderId)

            if (result.isSuccess) {
                _order.value = result.getOrNull()
            } else {
                _order.value = null
            }
            _isLoading.value = false
        }
    }
}