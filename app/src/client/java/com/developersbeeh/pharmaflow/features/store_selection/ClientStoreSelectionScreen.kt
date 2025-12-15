package com.developersbeeh.pharmaflow.features.store_selection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.model.Store
import com.developersbeeh.pharmaflow.data.repository.StoreRepository
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientStoreViewModel @Inject constructor(
    private val repository: StoreRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    var stores by mutableStateOf<List<Store>>(emptyList())
    var isLoading by mutableStateOf(true)

    init { loadStores() }

    private fun loadStores() {
        viewModelScope.launch {
            isLoading = true
            val result = repository.getAllStores()
            stores = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    fun selectStore(store: Store, onStoreSelected: () -> Unit) {
        sessionManager.setStore(store)
        onStoreSelected()
    }
}

@Composable
fun ClientStoreSelectionScreen(
    onStoreSelected: () -> Unit,
    viewModel: ClientStoreViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // HEADER VISUAL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bem-vindo ao PharmaFlow", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Text("Escolha a unidade mais próxima", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            }
        }

        // LISTA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-20).dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            if (viewModel.isLoading) {
                LoadingState("Localizando farmácias...")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text("Nossas Lojas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }
                    items(viewModel.stores) { store ->
                        ClientStoreCard(store) { viewModel.selectStore(store, onStoreSelected) }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun ClientStoreCard(store: Store, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Logo da Loja (Se houver) ou Ícone Padrão
            if (store.logoUrl.isNotEmpty()) {
                AsyncImage(model = store.logoUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = store.address.ifEmpty { "Endereço não informado" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}