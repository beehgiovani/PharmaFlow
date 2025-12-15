package com.developersbeeh.pharmaflow.features.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developersbeeh.pharmaflow.R
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    onBackClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onImportClick: () -> Unit,
    viewModel: StockListViewModel = hiltViewModel()
) {
    // Coleta os itens paginados (Paging 3)
    val pagedProducts = viewModel.pagedProducts.collectAsLazyPagingItems()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterLowStock by viewModel.filterLowStock.collectAsState()
    val filterNoEan by viewModel.filterNoEan.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val refreshTrigger by viewModel.refreshTrigger.collectAsState()

    var active by rememberSaveable { mutableStateOf(false) }

    var isCondensed by rememberSaveable { mutableStateOf(true) } // Default to condensed for power users

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            pagedProducts.refresh()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            pagedProducts.refresh()
        }
    }

    Scaffold(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gerenciar Estoque")
                        if (pagedProducts.itemCount > 0) {
                            Text("${pagedProducts.itemCount}+ itens carregados", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Toggle Condensed Mode
                    IconButton(onClick = { isCondensed = !isCondensed }) {
                        Icon(
                            if (isCondensed) Icons.Default.ViewModule else Icons.Default.ViewList,
                            contentDescription = "Alternar Visualização"
                        )
                    }
                    IconButton(onClick = { pagedProducts.refresh() }) {
                        Icon(Icons.Default.Refresh, "Recarregar")
                    }
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Importar", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProductClick,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(bottom = 80.dp)) {

            // --- BARRA DE BUSCA ---
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { active = false },
                    active = false,
                    onActiveChange = { active = it },
                    placeholder = { Text("Nome ou EAN...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {}
            }

            // --- FILTROS ---
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterLowStock,
                        onClick = { viewModel.toggleLowStockFilter(!filterLowStock) },
                        label = { Text("Estoque Crítico") },
                        leadingIcon = if (filterLowStock) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
                    )
                }
                item {
                    FilterChip(
                        selected = filterNoEan,
                        onClick = { viewModel.toggleNoEanFilter(!filterNoEan) },
                        label = { Text("Sem EAN") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                    )
                }

                items(viewModel.categories.size) { index ->
                    val cat = viewModel.categories[index]
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = { viewModel.onCategorySelected(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- LISTA PAGINADA ---
            Box(modifier = Modifier.fillMaxSize()) {

                if (pagedProducts.loadState.refresh is LoadState.Error) {
                    val e = pagedProducts.loadState.refresh as LoadState.Error
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Erro ao carregar dados",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = e.error.localizedMessage ?: "Erro desconhecido no Firestore.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { pagedProducts.retry() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                else if (pagedProducts.loadState.refresh is LoadState.Loading) {
                    LoadingState("Carregando estoque...")
                }
                else if (pagedProducts.itemCount == 0) {
                    EmptyState(
                        message = if(searchQuery.isEmpty()) "Nenhum produto cadastrado nesta loja." else "Nada encontrado para '$searchQuery'.",
                        actionLabel = "Cadastrar Novo",
                        onAction = onAddProductClick
                    )
                }
                else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = pagedProducts.itemCount,
                            key = pagedProducts.itemKey { it.id },
                            contentType = pagedProducts.itemContentType { "product" }
                        ) { index ->
                            val product = pagedProducts[index]
                            if (product != null) {
                                ProductItemCard(
                                    product = product,
                                    onClick = { onEditProductClick(product.id) },
                                    onToggleVisibility = {
                                        viewModel.toggleVisibility(product)
                                    },
                                    isCondensed = isCondensed
                                )
                            }
                        }

                        if (pagedProducts.loadState.append is LoadState.Loading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductItemCard(
    product: Product,
    onClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    isCondensed: Boolean = false
) {
    val context = LocalContext.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onClick()
                    false // Snap back
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isCondensed) {
                        onToggleVisibility()
                    }
                    false // Snap back
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer // Edit (Left to Right)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer // Visibility (Right to Left)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    Icon(
                        if (product.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Visibilidade",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        content = {
            // CARD CONTENT
            // CARD CONTENT
            val cardColor = when {
                !product.isActive -> MaterialTheme.colorScheme.surfaceVariant // Opaque Grey
                product.isExpired() -> MaterialTheme.colorScheme.errorContainer // Opaque Redish
                product.isExpiringSoon() -> Color(0xFFFFF9C4) // Opaque Yellowish
                product.stockQuantity < 5 -> MaterialTheme.colorScheme.errorContainer // Opaque Redish
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                elevation = CardDefaults.cardElevation(if (isCondensed) 0.dp else 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth().clickable { onClick() }
            ) {
                if (isCondensed) {
                    // --- CONDENSED LAYOUT ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (product.stockQuantity < 5) {
                                     Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                     Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "Estoque: ${product.stockQuantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (product.stockQuantity < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Text(
                            text = "R$ ${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // --- FULL LAYOUT (Standard) ---
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(product.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            placeholder = painterResource(id = R.drawable.img_placeholder),
                            error = painterResource(id = R.drawable.img_placeholder),
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            alpha = if (product.isActive) 1f else 0.5f
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2
                            )
                            
                             if (!product.isActive) {
                                Text("INATIVO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            } else {
                                Text("EAN: ${product.ean.ifEmpty { "N/A" }}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Qtd: ${product.stockQuantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (product.stockQuantity < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = onToggleVisibility) {
                                Icon(
                                    if (product.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Visibilidade",
                                    tint = if (product.isActive) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        
                             if (product.isOnSale()) {
                                Text("R$ ${String.format("%.2f", product.price)}", style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.LineThrough, color = Color.Gray)
                                Text("R$ ${String.format("%.2f", product.promoPrice)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("R$ ${String.format("%.2f", product.price)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    )
}