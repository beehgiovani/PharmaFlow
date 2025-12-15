package com.developersbeeh.pharmaflow.features.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersHistoryScreen(
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Pedidos") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                LoadingState("Buscando histórico...")
            } else if (orders.isEmpty()) {
                EmptyState(
                    message = "Você ainda não fez nenhum pedido.",
                    icon = Icons.Default.History
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = orders, key = { it.firestoreId }) { order ->
                        ClientOrderCard(
                            order = order,
                            onClick = { orderId ->
                                onNavigateToDetail(orderId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientOrderCard(
    order: Order,
    onClick: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val converter = Converters()
    // Tenta converter itensJson ou usa lista vazia se falhar
    val items = try {
        converter.fromString(order.itemsJson)
    } catch (e: Exception) {
        emptyList()
    }

    val isCanceled = order.status == "Cancelado"

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(order.firestoreId) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Loja + Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = order.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pedido #${order.firestoreId.takeLast(5).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = dateFormat.format(Date(order.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TRACKING STEPPER ---
            if (!isCanceled) {
                OrderTrackingStepper(currentStatus = order.status)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pedido Cancelado", color = Color.Red, fontWeight = FontWeight.Bold)
                        if (order.cancellationReason.isNotEmpty()) {
                            Text(
                                text = "Motivo: ${order.cancellationReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Lista resumida de itens (Top 3)
            items.take(3).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Medication,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.quantity}x ${item.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "R$ ${String.format("%.2f", item.price * item.quantity)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (items.size > 3) {
                Text(
                    "+ ${items.size - 3} outros itens...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Totais e Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tipo de entrega ícone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (order.isDelivery) Icons.Default.LocalShipping else Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (order.isDelivery) "Entrega" else "Retirada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Total: R$ ${String.format("%.2f", order.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// COMPONENTE STEPPER (BARRA DE PROGRESSO) - Fica aqui e é público
@Composable
fun OrderTrackingStepper(currentStatus: String) {
    val steps = listOf("Pendente", "Em Separação", "Saiu para Entrega", "Entregue")
    val currentStepIndex = steps.indexOfFirst { it.equals(currentStatus, ignoreCase = true) }
        .let { if (it == -1) 0 else it }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, stepName ->
            val isActive = index <= currentStepIndex
            val color by animateColorAsState(
                if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray,
                label = "colorAnim"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStepIndex) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (index == currentStepIndex) {
                        Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val label = when (stepName) {
                    "Pendente" -> "Recebido"
                    "Em Separação" -> "Separando"
                    "Saiu para Entrega" -> "Saiu"
                    "Entregue" -> "Entregue"
                    else -> ""
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = if (index == currentStepIndex) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < steps.size - 1) {
                val lineColor =
                    if (index < currentStepIndex) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(bottom = 14.dp)
                        .background(lineColor)
                )
            }
        }
    }
}