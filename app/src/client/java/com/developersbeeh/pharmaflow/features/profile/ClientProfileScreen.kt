package com.developersbeeh.pharmaflow.features.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.model.User
import com.developersbeeh.pharmaflow.features.auth.AuthRepository
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.getCurrentUser()
            if (result.isSuccess) _user.value = result.getOrNull()
            _isLoading.value = false
        }
    }

    fun updateProfile(whatsapp: String, address: String, neighborhood: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.updateUserProfile(whatsapp, address, neighborhood)
            if (result.isSuccess) loadProfile()
            _isLoading.value = false
            onResult(result.isSuccess)
        }
    }

    fun logout(onLogout: () -> Unit) {
        authRepository.logout()
        onLogout()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var whatsapp by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var showNeighborhoodDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            whatsapp = it.whatsapp
            address = it.address
            neighborhood = it.neighborhood
        }
    }

    // ... (Scaffold structure omitted, keeping it clean)
    // To do clean replace, I must target specific blocks. 
    // This tool call is becoming too large/risky.
    // I will return to smaller edits.


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = { IconButton(onClick = { viewModel.logout(onLogoutSuccess) }) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = MaterialTheme.colorScheme.error) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && user == null) {
                LoadingState("Carregando perfil...")
            } else {
                user?.let { currentUser ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = currentUser.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = currentUser.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                        Spacer(modifier = Modifier.height(32.dp))

                        // Cartão Fidelidade
                        Card(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))).padding(24.dp)) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("PharmaPoints", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("${currentUser.points}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("pontos", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    }
                                }
                            }
                        }

                        Text("A cada R$ 1,00 = 1 Ponto. Use para descontos!", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                        // Formulário
                        Text("Meus Dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(12.dp))
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço Padrão") }, leadingIcon = { Icon(Icons.Default.Home, null) }, modifier = Modifier.fillMaxWidth(), maxLines = 3, shape = RoundedCornerShape(12.dp))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bairro (Clicável para abrir Dialog)
                        OutlinedTextField(
                            value = neighborhood, 
                            onValueChange = {}, 
                            enabled = false, // Read-only, clicável via box ou modifier
                            label = { Text("Bairro (Toque para alterar)") }, 
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) }, 
                            modifier = Modifier.fillMaxWidth().clickable { showNeighborhoodDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        if (showNeighborhoodDialog) {
                            val neighborhoods = com.developersbeeh.pharmaflow.utils.GuarujaUtils.getNeighborhoods()
                            AlertDialog(
                                onDismissRequest = { showNeighborhoodDialog = false },
                                title = { Text("Selecione seu Bairro") },
                                text = {
                                    LazyColumn(modifier = Modifier.heightIn(max=300.dp)) {
                                        items(neighborhoods) { bairro ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        neighborhood = bairro
                                                        showNeighborhoodDialog = false
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.LocationOn, null, tint=MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(12.dp))
                                                Text(bairro, style=MaterialTheme.typography.bodyLarge)
                                            }
                                            HorizontalDivider()
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showNeighborhoodDialog = false }) { Text("Cancelar") } }
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.updateProfile(whatsapp, address, neighborhood) { success ->
                                    if (success) Toast.makeText(context, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Erro ao atualizar.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("SALVAR ALTERAÇÕES", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}