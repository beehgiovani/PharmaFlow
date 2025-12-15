package com.developersbeeh.pharmaflow.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.remote.BrasilApi
import com.developersbeeh.pharmaflow.data.remote.CepResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val brasilApi: BrasilApi
) : ViewModel() {

    // Endereço
    private val _addressResult = MutableStateFlow<CepResponse?>(null)
    val addressResult = _addressResult.asStateFlow()
    private val _isLoadingCep = MutableStateFlow(false)
    val isLoadingCep = _isLoadingCep.asStateFlow()

    // --- LOGIN CORRIGIDO (Retorna mensagem de erro ou null se sucesso) ---
    suspend fun loginUser(email: String, pass: String): String? {
        if (email.isBlank() || pass.isBlank()) return "Preencha e-mail e senha."

        return try {
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                null // Sucesso = Sem erro
            } else {
                // Pega a mensagem tratada no repository ou erro genérico
                result.exceptionOrNull()?.message ?: "Falha desconhecida no login."
            }
        } catch (e: Exception) {
            "Erro de conexão: ${e.message}"
        }
    }

    // --- CADASTRO (Retorna sucesso/falha e pode ser melhorado similar ao login no futuro) ---
    suspend fun registerUser(
        name: String, email: String, whatsapp: String,
        neighborhood: String, address: String, authorizedContact: Boolean,
        pass: String, role: String = "client"
    ): Boolean {
        val result = repository.registerUser(
            name, email, whatsapp, neighborhood, address,
            authorizedContact, pass, role
        )
        return result.isSuccess
    }

    // Bridge para Password Reset
    suspend fun resetPassword(email: String): Boolean {
        val result = repository.sendPasswordResetEmail(email)
        return result.isSuccess
    }

    // --- CEP (Mantido Igual) ---
    fun fetchAddressByCep(cep: String) {
        val cleanCep = cep.replace("[^0-9]".toRegex(), "")
        if (cleanCep.length != 8) return

        viewModelScope.launch {
            _isLoadingCep.value = true
            try {
                val response = brasilApi.getAddressByCep(cleanCep)
                _addressResult.value = response
            } catch (e: Exception) {
                _addressResult.value = null
            }
            _isLoadingCep.value = false
        }
    }

    fun clearAddressResult() { _addressResult.value = null }
}