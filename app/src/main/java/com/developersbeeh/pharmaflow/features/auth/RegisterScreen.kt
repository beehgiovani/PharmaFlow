package com.developersbeeh.pharmaflow.features.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.developersbeeh.pharmaflow.R
import com.developersbeeh.pharmaflow.utils.GuarujaUtils
import kotlinx.coroutines.launch
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Campos de Texto
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Endereço e CEP
    var cep by remember { mutableStateOf("") }
    var addressStreet by remember { mutableStateOf("") }
    var addressNumber by remember { mutableStateOf("") }

    // Dropdown Inteligente
    var selectedNeighborhood by remember { mutableStateOf("") }
    var expandedNeighborhood by remember { mutableStateOf(false) }

    // Flags
    var isMotoboyRegistration by remember { mutableStateOf(false) }
    var authorizeContact by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    // Observa dados da API de CEP
    val isLoadingCep by viewModel.isLoadingCep.collectAsState()
    val addressFound by viewModel.addressResult.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // Carrega bairros ordenados
    val neighborhoods = remember { GuarujaUtils.getNeighborhoods() }

    // --- REAGIR AO CEP ENCONTRADO ---
    LaunchedEffect(addressFound) {
        addressFound?.let { data ->
            // Preenche Rua (Usuário pode editar se estiver errado)
            if (!data.street.isNullOrEmpty()) {
                addressStreet = data.street
            }

            // Tenta adivinhar o bairro da nossa lista fechada
            if (!data.neighborhood.isNullOrEmpty()) {
                // Remove acentos e joga pra minúsculo para comparar melhor
                val apiBairro = normalizeText(data.neighborhood)

                // Procura na nossa lista oficial qual bairro contém o texto da API
                val match = neighborhoods.find {
                    val localBairro = normalizeText(it)
                    localBairro.contains(apiBairro) || apiBairro.contains(localBairro)
                }

                if (match != null) {
                    selectedNeighborhood = match
                    Toast.makeText(context, "Bairro detectado: $match", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Endereço carregado! Por favor, confirme seu Bairro.", Toast.LENGTH_LONG).show()
                }
            }
            viewModel.clearAddressResult() // Limpa para não sobrescrever caso o usuário apague e digite outro
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = R.drawable.logo_pharmaflow), contentDescription = "Logo", modifier = Modifier.width(180.dp).height(80.dp), contentScale = ContentScale.Fit)

        Text("Crie sua conta", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))

        // CEP (O Acelerador - Não Obrigatório mas útil)
        OutlinedTextField(
            value = cep,
            onValueChange = {
                if (it.length <= 8) {
                    cep = it
                    // Dispara busca ao completar 8 digitos
                    if (it.length == 8) viewModel.fetchAddressByCep(it)
                }
            },
            label = { Text("CEP (Preenche automático)") },
            leadingIcon = { Icon(Icons.Default.MarkunreadMailbox, null) },
            trailingIcon = if(isLoadingCep) { { CircularProgressIndicator(modifier=Modifier.size(20.dp), strokeWidth = 2.dp) } } else null,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Text("Se não souber, deixe em branco e preencha abaixo.", fontSize=10.sp, color=Color.Gray, lineHeight=12.sp, modifier=Modifier.padding(bottom=16.dp))

        // Nome
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome Completo") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        // Bairro (O coração da logística)
        ExposedDropdownMenuBox(
            expanded = expandedNeighborhood,
            onExpandedChange = { expandedNeighborhood = !expandedNeighborhood },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedNeighborhood,
                onValueChange = {},
                readOnly = true,
                label = { Text("Bairro * (Define sua Loja)") },
                leadingIcon = { Icon(Icons.Default.Map, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNeighborhood) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expandedNeighborhood, onDismissRequest = { expandedNeighborhood = false }) {
                neighborhoods.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { selectedNeighborhood = item; expandedNeighborhood = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Rua
        OutlinedTextField(value = addressStreet, onValueChange = { addressStreet = it }, label = { Text("Nome da Rua") }, leadingIcon = { Icon(Icons.Default.Home, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        // Número
        OutlinedTextField(
            value = addressNumber,
            onValueChange = { addressNumber = it },
            label = { Text("Número / Complemento") },
            leadingIcon = { Icon(Icons.Default.Pin, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dados Login
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp (com DDD)") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isMotoboyRegistration = !isMotoboyRegistration }) {
            Checkbox(checked = isMotoboyRegistration, onCheckedChange = { isMotoboyRegistration = it })
            Text("Sou Entregador parceiro", style = MaterialTheme.typography.bodyMedium, fontWeight = if(isMotoboyRegistration) FontWeight.Bold else FontWeight.Normal)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val fullAddress = if (addressNumber.isNotBlank()) "$addressStreet, $addressNumber" else addressStreet

                if (name.isNotEmpty() && email.isNotEmpty() && whatsapp.isNotEmpty() && password.length >= 6 && selectedNeighborhood.isNotEmpty() && fullAddress.isNotEmpty()) {
                    isLoading = true
                    val roleToSave = if (isMotoboyRegistration) "driver_pending" else "client"

                    scope.launch {
                        val success = viewModel.registerUser(
                            name, email, whatsapp,
                            selectedNeighborhood,
                            fullAddress,
                            authorizeContact, password, roleToSave
                        )
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, if(isMotoboyRegistration) "Cadastro em análise!" else "Bem-vindo!", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            Toast.makeText(context, "Erro ao cadastrar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Preencha todos os campos obrigatórios (*)", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("CADASTRAR")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Já tem uma conta? Entre aqui.", modifier = Modifier.clickable { onBackToLogin() }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Utilitário para limpar string e facilitar o "fuzzy match" do bairro
fun normalizeText(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
}