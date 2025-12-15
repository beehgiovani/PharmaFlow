package com.developersbeeh.pharmaflow.features.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.User
import com.developersbeeh.pharmaflow.data.repository.StaffRepository
import com.developersbeeh.pharmaflow.utils.GuarujaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val repository: StaffRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _staffList = MutableStateFlow<List<User>>(emptyList())
    val staffList = _staffList.asStateFlow()

    private val _pendingList = MutableStateFlow<List<User>>(emptyList())
    val pendingList = _pendingList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchResult = MutableStateFlow<User?>(null)
    val searchResult = _searchResult.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadStaffAndDrivers()
        loadPending()
    }

    // CORREÇÃO: Carrega Staff Fixo E Motoboys da Região
    private fun loadStaffAndDrivers() {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true

                // 1. Busca pessoal fixo (gerentes, balconistas desta loja)
                val storeStaffResult = repository.getStaffByStore(storeId)
                val storeStaff = storeStaffResult.getOrDefault(emptyList())

                // 2. Descobre qual é a zona desta loja
                val zone = GuarujaUtils.getDeliveryZoneForStore(storeId)

                // 3. Busca motoboys que atendem essa zona
                val driversResult = repository.getDriversByZone(zone)
                val drivers = driversResult.getOrDefault(emptyList())

                // 4. Combina e remove duplicados por segurança
                val combinedList = (storeStaff + drivers).distinctBy { it.id }

                _staffList.value = combinedList
                _isLoading.value = false
            }
        }
    }

    private fun loadPending() {
        viewModelScope.launch {
            val result = repository.getPendingDrivers()
            if (result.isSuccess) {
                _pendingList.value = result.getOrDefault(emptyList())
            }
        }
    }

    fun searchUser(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.findUserByEmail(email)
            if (result.isSuccess) {
                _searchResult.value = result.getOrNull()
            }
            _isLoading.value = false
        }
    }

    fun clearSearch() { _searchResult.value = null }

    // --- AÇÕES ---

    fun approveDriver(user: User, zone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.approveDriver(user.id, zone)
            if (result.isSuccess) {
                // CORREÇÃO: Recarrega as listas para mover de "Pendente" para "Equipe"
                loadData()
            }
            _isLoading.value = false
        }
    }

    fun rejectDriver(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.rejectDriver(user.id)
            loadPending() // Apenas atualiza pendentes
            _isLoading.value = false
        }
    }

    fun promoteUser(user: User, role: String) {
        val storeId = sessionManager.getCurrentStoreId()
        if (storeId.isNotEmpty()) {
            viewModelScope.launch {
                _isLoading.value = true
                val result = repository.updateUserRole(user.id, role, storeId)
                if (result.isSuccess) {
                    loadStaffAndDrivers()
                    clearSearch()
                }
                _isLoading.value = false
            }
        }
    }

    fun removeUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.removeStaffAccess(user.id)
            if (result.isSuccess) {
                loadStaffAndDrivers()
            }
            _isLoading.value = false
        }
    }
}