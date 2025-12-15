package com.developersbeeh.pharmaflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.features.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Adicionei estado Pending para motoboys em aprovação
sealed class StartDestination {
    object Loading : StartDestination()
    object Login : StartDestination()
    object Home : StartDestination()
    data class AccessDenied(val reason: String = "") : StartDestination()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<StartDestination>(StartDestination.Loading)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            _startDestination.value = StartDestination.Loading

            if (authRepository.isUserLoggedIn()) {
                verifyUserRole()
            } else {
                _startDestination.value = StartDestination.Login
            }
        }
    }

    private suspend fun verifyUserRole() {
        // BuildConfig flags definidos no build.gradle
        val isAppAdmin = BuildConfig.IS_ADMIN
        val isAppMotoboy = BuildConfig.IS_MOTOBOY

        val userRole = authRepository.getUserRole()
        // Possíveis roles: 'client', 'manager', 'owner', 'driver', 'driver_pending'

        if (isAppAdmin) {
            // ADMIN: Só Manager/Owner/Admin
            if (userRole == "manager" || userRole == "owner" || userRole == "admin") {
                _startDestination.value = StartDestination.Home
            } else {
                _startDestination.value = StartDestination.AccessDenied("Esta conta não é administrativa.")
            }
        } else if (isAppMotoboy) {
            // MOTOBOY: Verifica status
            when (userRole) {
                "driver" -> _startDestination.value = StartDestination.Home
                "driver_pending" -> _startDestination.value = StartDestination.AccessDenied("Seu cadastro está em análise. Aguarde a aprovação do gerente.")
                else -> _startDestination.value = StartDestination.AccessDenied("Esta conta não é de entregador. Use o app de Clientes.")
            }
        } else {
            // CLIENTE: Aberto para todos, exceto motoboys pendentes/bloqueados
            if (userRole == "driver_pending") {
                // Opcional: Se quiser bloquear motoboy de comprar, mantenha negado.
                // Mas geralmente um motoboy também pode ser cliente nas folgas.
                // Vamos deixar passar como cliente normal aqui.
                _startDestination.value = StartDestination.Home
            } else {
                _startDestination.value = StartDestination.Home
            }
        }
    }

    fun logout() {
        authRepository.logout()
        checkAuthStatus()
    }
}