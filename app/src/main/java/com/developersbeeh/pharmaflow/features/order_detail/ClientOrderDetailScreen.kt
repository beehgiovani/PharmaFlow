package com.developersbeeh.pharmaflow.features.order_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.developersbeeh.pharmaflow.data.model.Converters
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.ui.components.ErrorState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import com.developersbeeh.pharmaflow.ui.components.OrderTrackingStepper // Importando do arquivo que criamos no Main
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientOrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit,
    viewModel: ClientOrderDetailViewModel = hiltViewModel()
) {
    val order by viewModel.order.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedido #${orderId.take(6).uppercase()}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> LoadingState("Carregando detalhes...")
                order == null -> ErrorState("Detalhes não encontrados ou sem permissão.") { viewModel.loadOrder(orderId) }
                else -> OrderDetailContent(order = order!!)
            }
        }
    }
}

@Composable
fun OrderDetailContent(order: Order) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val converter = Converters()
    val items = try { converter.fromString(order.itemsJson) } catch (e: Exception) { emptyList() }
    val isCanceled = order.status == "Cancelado"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status Atual", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                if (!isCanceled) {
                    OrderTrackingStepper(currentStatus = order.status)
                } else {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Pedido Cancelado", color = Color.Red, fontWeight = FontWeight.Bold)
                            if (order.cancellationReason.isNotEmpty()) Text("Motivo: ${order.cancellationReason}", color = Color.Red)
                        }
                    }
                }
            }
        }

        // Informações Loja
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Detalhes", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(order.storeName, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data: ${dateFormat.format(Date(order.date))}")
                }
            }
        }

        // Entrega
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Logística", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(if(order.isDelivery) Icons.Default.LocalShipping else Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(if(order.isDelivery) "Entrega no endereço:" else "Retirada na Loja", fontWeight = FontWeight.SemiBold)
                        if(order.isDelivery) Text(order.deliveryAddress)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pagamento: ${order.paymentMethod}")
            }
        }

        // Itens
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Produtos", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.name}", maxLines = 1, modifier = Modifier.weight(1f))
                            Text("R$ ${String.format("%.2f", item.price * item.quantity)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Final", fontWeight = FontWeight.Bold)
                    Text("R$ ${String.format("%.2f", order.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}