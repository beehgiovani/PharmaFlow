package com.developersbeeh.pharmaflow.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import com.developersbeeh.pharmaflow.features.auth.AuthRepository // <--- NOVO
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository // <--- INJEÇÃO NOVA
) : ViewModel() {

    val currentStore = sessionManager.currentStore

    private val _totalSales = MutableStateFlow(0.0)
    val totalSales = _totalSales.asStateFlow()

    private val _orderCount = MutableStateFlow(0)
    val orderCount = _orderCount.asStateFlow()

    private val _lowStockCount = MutableStateFlow(0L)
    val lowStockCount = _lowStockCount.asStateFlow()

    // Produtor de dados para o gráfico Vico
    val modelProducer = CartesianChartModelProducer()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                // 1. Estoque Baixo
                val stockResult = productRepository.getLowStockCount(storeId, 5)
                if (stockResult.isSuccess) {
                    _lowStockCount.value = stockResult.getOrDefault(0L)
                }

                // 2. Vendas da Semana
                val result = orderRepository.getRecentOrders(storeId, 7) // Assumindo que este método existe no repo ou foi ajustado

                if (result.isSuccess) {
                    val allOrders = result.getOrDefault(emptyList())
                    // Filtra apenas finalizados/ativos válidos (não cancelados) para a soma
                    val validOrders = allOrders.filter { it.status != "Cancelado" }

                    _totalSales.value = validOrders.sumOf { it.totalAmount }
                    _orderCount.value = validOrders.size

                    // Preparar dados para o gráfico (Soma por dia da semana)
                    val salesByDay = mutableMapOf<Int, Double>()
                    // Inicializa os últimos 7 dias com 0
                    val calendar = Calendar.getInstance()
                    for (i in 0..6) {
                        salesByDay[calendar.get(Calendar.DAY_OF_YEAR)] = 0.0
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    }

                    // Agrupa vendas
                    validOrders.forEach { order ->
                        val c = Calendar.getInstance()
                        c.timeInMillis = order.date
                        val dayOfYear = c.get(Calendar.DAY_OF_YEAR)
                        val currentTotal = salesByDay.getOrDefault(dayOfYear, 0.0)
                        salesByDay[dayOfYear] = currentTotal + order.totalAmount
                    }

                    // Gera os dados para o gráfico Vico
                    val sortedData = salesByDay.entries.sortedBy { it.key }
                    val xValues: List<Number> = sortedData.mapIndexed { index, _ -> index }
                    val yValues: List<Number> = sortedData.map { it.value }

                    modelProducer.runTransaction {
                        columnSeries {
                            series(x = xValues, y = yValues)
                        }
                    }
                }
            }
        }
    }

    // --- FUNÇÃO LOGOUT ---
    fun logout(onLogoutSuccess: () -> Unit) {
        authRepository.logout()
        sessionManager.clearSession()
        onLogoutSuccess()
    }
}