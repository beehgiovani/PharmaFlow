package com.developersbeeh.pharmaflow.features.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@HiltViewModel
class StockListViewModel
@Inject
constructor(private val repository: ProductRepository, private val sessionManager: SessionManager) :
        ViewModel() {

    // Estados de Filtro
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterLowStock = MutableStateFlow(false)
    val filterLowStock = _filterLowStock.asStateFlow()

    private val _filterNoEan = MutableStateFlow(false)
    val filterNoEan = _filterNoEan.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Gatilho para forçar recarga da lista
    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow() // <--- CORREÇÃO: Agora é público!

    // Categorias Oficiais
    val categories =
            listOf(
                    "Todos",
                    "MEDICAMENTO",
                    "GENERICO",
                    "SIMILAR",
                    "ETICO",
                    "PERFUMARIA",
                    "CONVENIENCIA",
                    "VITAMINAS",
                    "SUPLEMENTOS",
                    "DERMOCOSMETICOS",
                    "CORRELATOS",
                    "FITOTERAPICO",
                    "HOMEOPATICO",
                    "NATURAIS",
                    "LIBERADO"
            )

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pagedProducts: Flow<PagingData<Product>> =
            combine(
                            _searchQuery.debounce(500),
                            _filterLowStock,
                            _filterNoEan,
                            _selectedCategory,
                            _refreshTrigger
                    ) { query, lowStock, noEan, category, _ ->
                        Quadruple(query, lowStock, noEan, category)
                    }
                    .flatMapLatest { (query, lowStock, noEan, category) ->
                        val storeId = sessionManager.getCurrentStoreId()

                        // Paging Source agora lida com Categoria e Busca e LowStock no servidor
                        repository.getPagedProducts(
                                        storeId,
                                        query,
                                        category,
                                        filterLowStock = lowStock
                                )
                                .map { pagingData ->
                                    var data = pagingData
                                    // Filtros locais remanescentes
                                    if (noEan) data = data.filter { it.ean.length < 8 }

                                    data
                                }
                    }
                    .cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _filterLowStock.value = false
        _filterNoEan.value = false
    }

    fun toggleLowStockFilter(enabled: Boolean) {
        _filterLowStock.value = enabled
        if (enabled) {
            _filterNoEan.value = false
            _selectedCategory.value = "Todos"
        }
    }

    fun toggleNoEanFilter(enabled: Boolean) {
        _filterNoEan.value = enabled
        if (enabled) {
            _filterLowStock.value = false
            _selectedCategory.value = "Todos"
        }
    }

    fun toggleVisibility(product: Product) {
        viewModelScope.launch {
            val newStatus = !product.isActive
            val result = repository.toggleProductVisibility(product.id, newStatus)

            if (result.isSuccess) {
                delay(500)
                _refreshTrigger.value += 1
            }
        }
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
