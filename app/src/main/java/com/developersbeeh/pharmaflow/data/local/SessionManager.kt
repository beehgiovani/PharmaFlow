package com.developersbeeh.pharmaflow.data.local

import com.developersbeeh.pharmaflow.data.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    // Estado da loja atual selecionada
    private val _currentStore = MutableStateFlow<Store?>(null)
    val currentStore = _currentStore.asStateFlow()

    fun setStore(store: Store) {
        _currentStore.value = store
    }

    fun clearSession() {
        _currentStore.value = null
    }

    // Helper para pegar o ID rápido (se nulo, retorna vazio)
    fun getCurrentStoreId(): String = _currentStore.value?.id ?: ""
}