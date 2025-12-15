package com.developersbeeh.pharmaflow.features.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.developersbeeh.pharmaflow.data.model.GlobalProduct
import com.developersbeeh.pharmaflow.data.repository.CatalogRepository
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- ViewModel ---
@HiltViewModel
class CatalogSearchViewModel @Inject constructor(
    private val repository: CatalogRepository
) : ViewModel() {
    var products by mutableStateOf<List<GlobalProduct>>(emptyList())
    var isLoading by mutableStateOf(false)

    init {
        search("") // Carrega tudo inicialmente
    }

    fun search(query: String) {
        viewModelScope.launch {
            isLoading = true
            val result = repository.searchGlobalProducts(query)
            products = result.getOrDefault(emptyList())
            isLoading = false
        }
    }
}

// --- Tela ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSearchScreen(
    onBackClick: () -> Unit,
    onProductSelected: (GlobalProduct) -> Unit,
    viewModel: CatalogSearchViewModel = hiltViewModel()
) {
    var query by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo Mestre") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Barra de Busca
            Box(modifier = Modifier.padding(16.dp)) {
                SearchBar(
                    query = query,
                    onQueryChange = {
                        query = it
                        viewModel.search(it)
                    },
                    onSearch = { active = false },
                    active = false,
                    onActiveChange = { active = it },
                    placeholder = { Text("Buscar no catálogo geral...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {}
            }

            if (viewModel.isLoading) {
                LoadingState("Pesquisando...")
            } else {
                if (viewModel.products.isEmpty()) {
                    EmptyState(
                        message = "Nenhum produto encontrado no catálogo global.",
                        icon = Icons.Default.Search
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.products) { globalProduct ->
                            CatalogItemCard(globalProduct) {
                                onProductSelected(globalProduct)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogItemCard(product: GlobalProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "EAN: ${product.ean}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Importar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}