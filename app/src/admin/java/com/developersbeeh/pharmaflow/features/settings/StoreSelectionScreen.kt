package com.developersbeeh.pharmaflow.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- VIEWMODEL LOCAL ---
@HiltViewModel
class StoreSelectionViewModel @Inject constructor(
    private val repository: StoreRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var stores by mutableStateOf<List<Store>>(emptyList())
    var isLoading by mutableStateOf(true)

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            isLoading = true
            val result = repository.getAllStores()
            if (result.isSuccess) {
                stores = result.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    fun addStore(name: String) {
        viewModelScope.launch {
            val result = repository.addStore(name)
            if (result.isSuccess) loadStores()
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            val result = repository.deleteStore(storeId)
            if (result.isSuccess) loadStores()
        }
    }

    fun selectStore(store: Store, onStoreSelected: () -> Unit) {
        sessionManager.setStore(store)
        onStoreSelected()
    }
}

// --- UI SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSelectionScreen(
    onStoreSelected: () -> Unit,
    viewModel: StoreSelectionViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }

    // Dialog para Adicionar Loja
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nova Loja") },
            text = {
                OutlinedTextField(
                    value = newStoreName,
                    onValueChange = { newStoreName = it },
                    label = { Text("Nome da Unidade") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStoreName.isNotEmpty()) {
                            viewModel.addStore(newStoreName)
                            newStoreName = ""
                            showAddDialog = false
                        }
                    }
                ) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecione a Unidade") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Loja", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (viewModel.isLoading) {
                LoadingState("Carregando filiais...")
            } else if (viewModel.stores.isEmpty()) {
                EmptyState("Nenhuma loja cadastrada.", icon = Icons.Default.Store)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.stores) { store ->
                        AdminStoreCard(
                            store = store,
                            onClick = { viewModel.selectStore(store, onStoreSelected) },
                            onDelete = { viewModel.deleteStore(store.id) }
                        )
                    }
                    // Espaço para o FAB não cobrir o último item
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun AdminStoreCard(store: Store, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Loja?") },
            text = { Text("Tem certeza que deseja excluir '${store.name}'? Isso não pode ser desfeito.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Logo ou Ícone Padrão
                if (store.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = store.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (store.address.isNotEmpty()) store.address else "Endereço não cadastrado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Gray)
            }
        }
    }
}