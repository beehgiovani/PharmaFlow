package com.developersbeeh.pharmaflow.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developersbeeh.pharmaflow.R
import com.developersbeeh.pharmaflow.data.model.Banner
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.features.cart.CartViewModel
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.PharmaCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
        onNavigateToCart: () -> Unit,
        onNavigateToDetail: (String) -> Unit,
        onNavigateToProfile: () -> Unit,
        onNavigateToHistory: () -> Unit,
        homeViewModel: ClientHomeViewModel = hiltViewModel(),
        cartViewModel: CartViewModel = hiltViewModel()
) {
    // Collect Paging Data
    val pagedProducts = homeViewModel.activePagedProducts.collectAsLazyPagingItems()

    val banners by homeViewModel.banners.collectAsState()
    val featuredProducts by homeViewModel.featuredProducts.collectAsState()
    val featuredLoading by homeViewModel.featuredLoading.collectAsState()

    val cartItems by cartViewModel.cartItems.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val currentStore by homeViewModel.storeState.collectAsState()
    val filterOptions by homeViewModel.filterOptions.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showNeighborhoodDialog by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentUser by homeViewModel.currentUser.collectAsState()
    
    // VERIFICAÇÃO DE DADOS (IMPEDE USO SE INCOMPLETO)
    var showMissingDataDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val needsUpdate = currentUser!!.neighborhood.isNullOrEmpty() || 
                              currentUser!!.neighborhood == "Unknown" ||
                              currentUser!!.whatsapp.isNullOrEmpty()
            showMissingDataDialog = needsUpdate
        }
    }

    if (showMissingDataDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {}, // Bloqueante: não fecha tocando fora
            title = { Text("Complete seu Cadastro") },
            text = { Text("Para garantir a entrega correta, precisamos que você informe seu Bairro e Whatsapp.\n\nPor favor, atualize seu perfil.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { 
                    showMissingDataDialog = false 
                    onNavigateToProfile()
                }) {
                    Text("Atualizar Agora")
                }
            },
            icon = { Icon(Icons.Default.AccountCircle, null) }
        )
    }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(
                        modifier =
                                Modifier.background(MaterialTheme.colorScheme.surface)
                                        .padding(bottom = 12.dp)
                ) {
                    // Header (Logo + Ícones)
                    TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showNeighborhoodDialog = true }
                                ) {
                                    AsyncImage(
                                            model = currentStore?.logoUrl?.takeIf { it.isNotEmpty() }
                                                            ?: R.drawable.logo_pharmaflow,
                                            contentDescription = "Logo",
                                            modifier = Modifier.height(32.dp).width(110.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    if (currentStore != null) {
                                        Column {
                                            Text(
                                                text = currentStore!!.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = currentStore!!.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val isOpen = com.developersbeeh.pharmaflow.utils.StoreStatusHelper.isStoreOpen(currentStore!!)
                                            val statusColor = if(isOpen) Color(0xFF4CAF50) else Color.Red
                                            val statusText = if(isOpen) "Aberto • Fecha às ${currentStore!!.closingTime}" else "FECHADO • Abre às ${currentStore!!.openingTime}"

                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                    )
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
                    // Header (Address/Cart)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Entregar em",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Casa", // TODO: Get from user address
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToCart) {
                            BadgedBox(badge = {
                                if (cartItems.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) { Text("${cartItems.sumOf { it.quantity }}") }
                                }
                            }) {
                                Icon(
                                    Icons.Outlined.ShoppingCart,
                                    contentDescription = "Carrinho",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Search Bar + Filter Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                            query = query,
                            onQueryChange = {
                                query = it
                                homeViewModel.onSearchQueryChanged(it)
                            },
                            onSearch = { homeViewModel.onSearchQueryChanged(it) },
                            active = false,
                            onActiveChange = {},
                            placeholder = { Text("Buscar produtos...") },
                            modifier = Modifier.weight(1f),
                            colors = SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        ) {}

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .size(50.dp) // Match SearchBar height approx
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Filtros",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Categorias
                    LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(homeViewModel.categories) { category ->
                            FilterChip(
                                    selected = category == selectedCategory,
                                    onClick = { homeViewModel.onCategorySelected(category) },
                                    label = { Text(category) },
                                    colors =
                                            FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor =
                                                            MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor =
                                                            MaterialTheme.colorScheme.onPrimary
                                            ),
                                    border = null,
                                    shape = CircleShape
                            )
                        }
                    }
                }
            }
        },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
            ) {

                // 1. CARROSSEL BANNERS
                if (query.isEmpty() && selectedCategory == "Todos" && banners.isNotEmpty()) {
                    item { BannerCarousel(banners) }
                }

                // 2. DESTAQUES (HORIZONTAL)
                if (query.isEmpty() && selectedCategory == "Todos" && featuredProducts.isNotEmpty()
                ) {
                    item {
                        Column {
                            Text(
                                    "Ofertas Especiais \uD83D\uDD25",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                            LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featuredProducts) { product ->
                                    PharmaCard(
                                            modifier = Modifier.width(140.dp),
                                            onClick = { onNavigateToDetail(product.id) }
                                    ) { FeaturedProductContent(product) }
                                }
                            }
                        }
                    }
                }

                // 3. TÍTULO TODOS OS PRODUTOS
                item {
                    Text(
                            if (query.isEmpty()) "Todos os Produtos" else "Resultados da Busca",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // 4. GRID DE PRODUTOS (MANUAL GRDI EM LAZY COLUMN)
                if (pagedProducts.loadState.refresh is LoadState.Loading) {
                    item {
                        Box(
                                Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                } else if (pagedProducts.itemCount == 0 &&
                                pagedProducts.loadState.refresh !is LoadState.Loading
                ) {
                    item {
                        EmptyState("Nenhum produto encontrado.", icon = Icons.Default.Medication)
                    }
                } else {
                    val itemCount = pagedProducts.itemCount
                    val rows = (itemCount + 1) / 2

                    items(rows) { rowIndex ->
                        Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val firstIndex = rowIndex * 2
                            val secondIndex = firstIndex + 1

                            // Item 1
                            if (firstIndex < itemCount) {
                                val p1 = pagedProducts[firstIndex]
                                if (p1 != null) {
                                    Box(Modifier.weight(1f)) {
                                        ProductGridCard(
                                                product = p1,
                                                onClick = { onNavigateToDetail(p1.id) },
                                                onBuyClick = {
                                                    cartViewModel.addToCart(p1)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                                "Adicionado ao carrinho!"
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }

                            // Item 2
                            if (secondIndex < itemCount) {
                                val p2 = pagedProducts[secondIndex]
                                if (p2 != null) {
                                    Box(Modifier.weight(1f)) {
                                        ProductGridCard(
                                                product = p2,
                                                onClick = { onNavigateToDetail(p2.id) },
                                                onBuyClick = {
                                                    cartViewModel.addToCart(p2)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                                "Adicionado ao carrinho!"
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            } else {
                                // Se for o último item da linha ímpar, preenche com espaço
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    if (pagedProducts.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }


    if (showFilterSheet) {
        FilterBottomSheet(
            currentOptions = filterOptions,
            onApply = {
                homeViewModel.updateFilters(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

// === COMPONENTES VISUAIS ===

@Composable
fun BannerCarousel(banners: List<Banner>) {
    LazyRow(
            modifier = Modifier.fillMaxWidth().height(220.dp), // Increased Height for Hero effect
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(banners) { banner ->
            PharmaCard(
                    modifier = Modifier.width(340.dp).fillMaxHeight(),
                    shape = MaterialTheme.shapes.large
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                            model =
                                    ImageRequest.Builder(LocalContext.current)
                                            .data(banner.imageUrl)
                                            .crossfade(true)
                                            .build(),
                            contentDescription = "Oferta",
                            contentScale = ContentScale.Crop, // Immersive crop
                            modifier = Modifier.fillMaxSize(),
                            placeholder = painterResource(id = R.drawable.img_placeholder),
                            error = painterResource(id = R.drawable.img_placeholder)
                    )
                    // Glass Overlay for text (Optional)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        // Title could go here if Banner model had it
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedProductContent(product: Product) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Box(contentAlignment = Alignment.TopStart) {
            AsyncImage(
                    model =
                            ImageRequest.Builder(LocalContext.current)
                                    .data(product.imageUrl)
                                    .crossfade(true)
                                    .build(),
                    contentDescription = null,
                    placeholder = painterResource(id = R.drawable.img_placeholder),
                    error = painterResource(id = R.drawable.img_placeholder),
                    fallback = painterResource(id = R.drawable.img_placeholder),
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit
            )
            DiscountBadge(product)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        PriceText(product)
    }
}

@Composable
fun ProductGridCard(product: Product, onClick: () -> Unit, onBuyClick: () -> Unit) {
    PharmaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Container with subtle background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // Taller image area
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(id = R.drawable.img_placeholder)
                )
                // Floating Discount Badge
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    DiscountBadge(product)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Info
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = product.category.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column { PriceText(product) }

                // Premium "Add" Button
                FilledTonalIconButton(
                    onClick = onBuyClick,
                    modifier = Modifier.size(40.dp), // Larger touch target
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "Adicionar",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DiscountBadge(product: Product) {
    if (product.isOnSale()) {
        val discount = ((1 - (product.promoPrice / product.price)) * 100).toInt()
        Surface(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(2.dp)
        ) {
            Text(
                    "-$discount%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun PriceText(product: Product) {
    if (product.isOnSale()) {
        Text(
                "R$ ${String.format("%.2f", product.price)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textDecoration = TextDecoration.LineThrough
        )
        Text(
                "R$ ${String.format("%.2f", product.promoPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
        )
    } else {
        Text(
                "R$ ${String.format("%.2f", product.price)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )
    }
}
