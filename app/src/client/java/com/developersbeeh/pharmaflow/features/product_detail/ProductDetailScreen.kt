package com.developersbeeh.pharmaflow.features.product_detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developersbeeh.pharmaflow.R
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.ui.components.ErrorState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import com.developersbeeh.pharmaflow.ui.components.PharmaButton
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    // Função de compartilhamento
    fun shareProduct(product: Product) {
        val shareText = buildString {
            append("💊 *${product.name}*\n")
            if (product.isOnSale()) {
                append("De R$ ${String.format("%.2f", product.price)} por *R$ ${String.format("%.2f", product.promoPrice)}*\n")
            } else {
                append("💰 R$ ${String.format("%.2f", product.price)}\n")
            }
            append("\nCompre agora na PharmaFlow!")
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Produto")
        context.startActivity(shareIntent)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { }, // Título vazio para foco na imagem
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState is ProductDetailUiState.Success) {
                        val product = (uiState as ProductDetailUiState.Success).product
                        IconButton(
                            onClick = { shareProduct(product) },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (uiState is ProductDetailUiState.Success) {
                val product = (uiState as ProductDetailUiState.Success).product
                ProductBottomBar(
                    product = product,
                    quantity = quantity,
                    onAddClick = {
                        viewModel.addToCart(product)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "${quantity}x ${product.name} adicionado!",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onIncrement = { viewModel.incrementQuantity() },
                    onDecrement = { viewModel.decrementQuantity() }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding()) // Importante para não esconder atrás da barra
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is ProductDetailUiState.Loading -> LoadingState()
                is ProductDetailUiState.Error -> ErrorState(state.message) { viewModel.loadProduct(productId) }
                is ProductDetailUiState.Success -> ProductContent(state.product)
            }
        }
    }
}

// Lógica de abertura da Bula (Google Search Intent)
fun openBula(context: Context, product: Product) {
    try {
        val query = "${product.name} bula anvisa"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.google.com/search?q=$encodedQuery"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir navegador", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ProductContent(product: Product) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- ÁREA DA IMAGEM ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                placeholder = painterResource(id = R.drawable.img_placeholder),
                error = painterResource(id = R.drawable.img_placeholder),
                fallback = painterResource(id = R.drawable.img_placeholder),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentScale = ContentScale.Fit
            )
        }

        // --- CONTEÚDO EM CARD ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp) // Efeito visual de sobreposição
                .background(
                    MaterialTheme.colorScheme.background,
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(24.dp)
        ) {
            // Linha com Categoria e Botão Bula
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.category.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // BOTÃO VER BULA
                OutlinedButton(
                    onClick = { openBula(context, product) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Bula / Info", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (product.ean.isNotEmpty()) {
                Text(
                    text = "EAN: ${product.ean}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preços com Destaque
            Row(verticalAlignment = Alignment.Bottom) {
                if (product.isOnSale()) {
                    Text(
                        text = "R$ ${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "R$ ${String.format("%.2f", product.promoPrice)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 32.sp
                    )
                } else {
                    Text(
                        text = "R$ ${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sobre o Produto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (product.description.isNotEmpty()) product.description else "Este produto não possui descrição detalhada. Consulte as informações na embalagem ou clique em 'Ver Bula' para mais detalhes.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Espaço extra para o scroll
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProductBottomBar(
    product: Product,
    quantity: Int,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Controlador de Quantidade
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = onDecrement) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Diminuir",
                        tint = if(quantity > 1) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Botão Adicionar com Preço Calculado
            val unitPrice = product.getCurrentPrice()
            val total = unitPrice * quantity
            
            PharmaButton(
                onClick = onAddClick,
                modifier = Modifier.weight(1f),
                text = "", // Custom content inside
                icon = {
                   Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                   Spacer(modifier = Modifier.width(8.dp))
                   Column(horizontalAlignment = Alignment.Start) {
                       Text("ADICIONAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                       Text("R$ ${String.format("%.2f", total)}", fontSize = 14.sp)
                   }
                }
            )
        }
    }
}