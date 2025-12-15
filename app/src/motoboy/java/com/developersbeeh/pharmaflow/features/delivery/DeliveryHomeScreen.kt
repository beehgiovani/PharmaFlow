package com.developersbeeh.pharmaflow.features.delivery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import com.developersbeeh.pharmaflow.ui.components.PharmaButton
import com.developersbeeh.pharmaflow.ui.components.PharmaCard
import com.developersbeeh.pharmaflow.ui.components.SwipeToConfirmButton
import com.developersbeeh.pharmaflow.ui.components.bouncingClickable
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHomeScreen(
        onNavigateToDetail: (String) -> Unit,
        onLogout: () -> Unit,
        viewModel: DeliveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            topBar = {
                TopAppBar(
                        title = { Text("Painel do Entregador") },
                        actions = {
                            IconButton(onClick = onLogout) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sair"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                                        titleContentColor = Color.White,
                                        actionIconContentColor = Color.White
                                )
                )
            },
            containerColor = Color(0xFFF5F5F5) // Fundo cinza claro para destacar os cards
    ) { padding ->
        Box(modifier = Modifier.padding(padding).padding(bottom = 80.dp)) {
            when (val state = uiState) {
                is DeliveryUiState.Loading -> LoadingState("Sincronizando rota...")
                is DeliveryUiState.NoZone ->
                        EmptyState(
                                message =
                                        "Cadastro em análise.\nSolicite ao gerente sua Zona de Entrega.",
                                icon = Icons.AutoMirrored.Filled.DirectionsBike
                        )
                is DeliveryUiState.Error ->
                        EmptyState(state.msg, Icons.AutoMirrored.Filled.DirectionsBike)
                is DeliveryUiState.Success -> {
                    // Nome do motorista (primeiro nome)
                    val rawName = state.activeDriver.name
                    val driverName = if (rawName.isNotBlank()) {
                         rawName.split(" ").firstOrNull() ?: rawName
                    } else {
                         "Entregador"
                    }

                    Column {
                        // Header Escuro Profissional
                        Surface(
                                color = MaterialTheme.colorScheme.inverseSurface,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                Text(
                                        "Olá, $driverName",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 24.sp
                                )
                                Text(
                                        "Zona Ativa: ${state.activeDriver.driverZone.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.LightGray
                                )
                            }
                        }

                        if (state.tasks.isEmpty()) {
                            Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                            "Sem entregas pendentes.",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                    )
                                    Text("Bom descanso!", color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("DEBUG INFO:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Zona: ${state.currentZone}", fontSize = 12.sp)
                                            Text("Lojas Monitoradas: ${state.monitoredStores.size}", fontSize = 12.sp)
                                            state.monitoredStores.forEach { 
                                                Text("- $it", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(state.tasks) { index, order ->
                                    // CARD ANIMADO (Efeito Cascata)
                                    PharmaCard(
                                            delayMillis =
                                                    index * 100, // Cada item entra 100ms depois do
                                            // anterior
                                            onClick = { onNavigateToDetail(order.firestoreId) },
                                            elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        DeliveryTaskContent(
                                                order = order,
                                                onAction = {
                                                    if (order.status == "Em Separação") {
                                                        viewModel.acceptDelivery(order)
                                                        Toast.makeText(
                                                                        context,
                                                                        "Pedido na mochila! Vai com cuidado.",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                    } else {
                                                        viewModel.completeDelivery(order)
                                                        Toast.makeText(
                                                                        context,
                                                                        "Entrega Finalizada!",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                    }
                                                }
                                        )
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(32.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Lógica de abertura de Apps de GPS (Waze/Google Maps)
fun openNavigationApp(context: Context, address: String) {
    try {
        // Formatação para garantir precisão no Guarujá
        val fullAddress = "$address, Guarujá - SP, Brasil"
        val encodedAddress = URLEncoder.encode(fullAddress, "UTF-8")

        // Tenta Google Maps
        val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedAddress")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback para Waze ou Navegador
            val wazeUri = Uri.parse("https://waze.com/ul?q=$encodedAddress")
            val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)
            context.startActivity(wazeIntent)
        }
    } catch (e: android.content.ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo de mapa instalado.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir mapa.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DeliveryTaskContent(order: Order, onAction: () -> Unit) {
    val context = LocalContext.current

    val isReadyToDeliver = order.status == "Saiu para Entrega"
    // Cores: Verde se está em rota, Azul se precisa pegar
    val statusBg = if (isReadyToDeliver) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
    val statusTxt = if (isReadyToDeliver) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
    val btnColor =
            if (isReadyToDeliver) ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)

    ElevatedCard(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation =
                    CardDefaults.cardElevation(
                            0.dp
                    ) // Já tem shadow no card pai, aqui só queremos o background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Linha Superior: Status e ID
            Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Surface(color = statusBg, shape = RoundedCornerShape(4.dp)) {
                    Text(
                            text = if (isReadyToDeliver) "EM ROTA" else "RETIRAR",
                            color = statusTxt,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                    )
                }
                Text(
                        text = "#${order.firestoreId.takeLast(5).uppercase()}",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linha do Tempo Visual (Loja -> Casa)
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Ícones e Linha Conectora
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(
                            Icons.Default.Store,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                    )
                    // Linha vertical pontilhada (simulada sólida aqui)
                    Box(modifier = Modifier.width(2.dp).weight(1f).background(Color.LightGray))
                    Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Textos
                Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Origem
                    Column {
                        Text(order.storeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ponto de Coleta", fontSize = 11.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Destino (Botão de Navegação Embutido)
                    Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp),
                            // Efeito 'Bouncing' ao clicar
                            modifier =
                                    Modifier.fillMaxWidth().bouncingClickable {
                                        openNavigationApp(context, order.deliveryAddress)
                                    }
                    ) {
                        Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(order.customerName, fontWeight = FontWeight.Bold)
                                Text(
                                        order.deliveryAddress,
                                        style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                        "Toque para abrir GPS",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Icon(
                                    Icons.Default.Navigation,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Informações de Pagamento (CRUCIAL PARA O MOTOBOY)
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
            ) {
                Text(
                        "COBRAR DO CLIENTE:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                )
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                            "R$ ${String.format("%.2f", order.totalAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                    )
                    Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                            shadowElevation = 1.dp
                    ) {
                        Text(
                                order.paymentMethod,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão de Ação Animado
            // Botão de Ação "Swipe to Confirm"
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SwipeToConfirmButton(
                    onClick = onAction,
                    text = if (isReadyToDeliver) "DESLIZAR P/ ENTREGAR" else "DESLIZAR P/ INICIAR",
                    containerColor = if (isReadyToDeliver) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    icon = if (isReadyToDeliver) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.DirectionsBike
                )
            }
        }
    }
}
