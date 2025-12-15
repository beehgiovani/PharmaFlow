package com.developersbeeh.pharmaflow.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Banner
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.repository.MarketingRepository
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.developersbeeh.pharmaflow.domain.sort.SortOption

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class ClientHomeViewModel
@Inject
constructor(
        private val productRepository: ProductRepository,
        private val marketingRepository: MarketingRepository,
        private val sessionManager: SessionManager,
        private val authRepository: com.developersbeeh.pharmaflow.features.auth.AuthRepository,
        private val storeRepository: com.developersbeeh.pharmaflow.data.repository.StoreRepository
) : ViewModel() {

    // Expose current store for UI
    val storeState = sessionManager.currentStore
    
    // Expose User for Data Validation
    val user = authRepository.getUserFlow()
        .asLiveData() // Optional or keep as Flow

    // Better as StateFlow for Compose
    val currentUser = authRepository.getUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners = _banners.asStateFlow()

    private val _featuredProducts = MutableStateFlow<List<Product>>(emptyList())
    val featuredProducts = _featuredProducts.asStateFlow()

    private val _featuredLoading = MutableStateFlow(true)
    val featuredLoading = _featuredLoading.asStateFlow()

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

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Controle de Debounce para busca
    private var searchJob: Job? = null

    // FILTROS AVANÇADOS (Search 2.0)
    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions = _filterOptions.asStateFlow()

    // PAGINAÇÃO PRINCIPAL
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedProducts: Flow<PagingData<Product>> =
            combine(_selectedCategory, _searchQuery, _filterOptions, storeState) { category, query, filters, store ->
                // Adicionamos 'store' ao combine para forçar re-emissão quando a loja mudar!
                Quadruple(category, query, filters, store)
            }
            .flatMapLatest { (category, query, filters, store) ->
                val storeId = store?.id ?: ""
                if (storeId.isNotEmpty()) {
                    productRepository.getPagedProducts(
                        storeId = storeId,
                        query = query,
                        category = category,
                        onlyActive = true,
                        filterLowStock = filters.onlyAvailable, 
                        sortOption = filters.sortBy,
                        onlyOnSale = filters.onlyOnSale,
                        onlyAvailable = filters.onlyAvailable
                    )
                } else {
                    kotlinx.coroutines.flow.emptyFlow()
                }
            }
            .cachedIn(viewModelScope)

    // Data Class auxiliar local (já que kotlin pair/triple limita em 3)
    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    // Expose active paged products (only active items)
    val activePagedProducts = pagedProducts

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val storeId = sessionManager.getCurrentStoreId()
            if (storeId.isNotEmpty()) {
                // 1. Banners
                val bannersRes = marketingRepository.getBanners(storeId)
                if (bannersRes.isSuccess) _banners.value = bannersRes.getOrDefault(emptyList())

                // 2. Featured (Top 10)
                _featuredLoading.value = true
                val featRes = productRepository.getFeaturedProducts(storeId)
                if (featRes.isSuccess) {
                    _featuredProducts.value = featRes.getOrDefault(emptyList())
                }
                _featuredLoading.value = false
            }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        if (_searchQuery.value.isNotEmpty()) {
            _searchQuery.value = ""
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _searchQuery.value = query
        }
    }

    fun updateFilters(newFilters: FilterOptions) {
        _filterOptions.value = newFilters
    }

    fun onNeighborhoodSelected(neighborhood: String) {
        viewModelScope.launch {
            // 1. Descobre qual loja atende esse bairro
            val targetStoreId = com.developersbeeh.pharmaflow.utils.GuarujaUtils.getPreferredStore(neighborhood)
            
            // 2. Busca os dados dessa loja
            val result = storeRepository.getStoreById(targetStoreId)
            
            if (result.isSuccess) {
                val store = result.getOrNull()
                if (store != null) {
                    // 3. Atualiza Sessão
                    sessionManager.setStore(store)
                    // 4. Recarrega dados (Banners, Destaques)
                    loadInitialData()
                    // Nota: O Flow combinando (pagedProducts) vai atualizar automaticamente pois ele não observa currentStoreID diretamente no combine,
                    // mas o ActivePagedProducts depende de um refresh.
                    // O ideal seria que o storeId fosse um STATEFLOW observado na query.
                    // Como activePagedProducts usa 'sessionManager.getCurrentStoreId()' dentro do flatMapLatest? 
                    // NÃO, ele usa no bloco, mas precisamos disparar o flatMap novamente.
                    // Solução rápida: Resetar filtro ou query para forçar update, ou melhor:
                    // Vamos dar um reload na UI.
                }
            }
        }
    }
}
