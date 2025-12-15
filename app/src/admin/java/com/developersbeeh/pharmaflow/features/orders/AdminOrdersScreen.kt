package com.developersbeeh.pharmaflow.features.orders

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.developersbeeh.pharmaflow.data.model.Converters
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.model.Store
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBackClick: () -> Unit,
    viewModel: AdminOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pendentes", "Histórico")

    var showPrinterDialog by remember { mutableStateOf(false) }
    var orderToPrint by remember { mutableStateOf<Order?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            viewModel.scanBluetoothDevices()
            showPrinterDialog = true
        } else {
            Toast.makeText(context, "Permissão Bluetooth necessária.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startPrintingFlow(order: Order) {
        orderToPrint = order
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            )
        } else {
            viewModel.scanBluetoothDevices()
            showPrinterDialog = true
        }
    }

    if (showPrinterDialog && orderToPrint != null) {
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Selecione a Impressora") },
            text = {
                if (uiState.bluetoothDevices.isEmpty()) {
                    Column {
                        Text("Nenhuma impressora pareada.", color = Color.Gray)
                        Button(
                            onClick = { viewModel.scanBluetoothDevices() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Buscar Dispositivos") }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max=300.dp)) {
                        items(uiState.bluetoothDevices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.printOrder(orderToPrint!!, device)
                                        Toast.makeText(context, "Imprimindo...", Toast.LENGTH_SHORT).show()
                                        showPrinterDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Print, null)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    // CORREÇÃO TRY CATCH
                                    val deviceName = try { device.name ?: device.address } catch(e: SecurityException) { "Dispositivo Bluetooth" }
                                    Text(deviceName, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrinterDialog = false }) { Text("Fechar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão de Pedidos") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    val count = if (index == 0) uiState.activeOrders.size else uiState.historyOrders.size
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text("$title ($count)") })
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (uiState.isLoading) LoadingState("Atualizando...")
                else {
                    val currentList = if (selectedTab == 0) uiState.activeOrders else uiState.historyOrders
                    if (currentList.isEmpty()) EmptyState("Nenhum pedido aqui.", icon = if(selectedTab==0) Icons.Default.CheckCircle else Icons.Default.History)
                    else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(currentList, key = { it.firestoreId }) { order ->
                                AdminOrderCard(
                                    order = order,
                                    availableStores = uiState.availableStores,
                                    onAdvance = { viewModel.advanceStatus(order); Toast.makeText(context,"Status Atualizado!",Toast.LENGTH_SHORT).show() },
                                    onCancel = { r -> viewModel.cancelOrder(order, r) },
                                    onTransfer = { store ->
                                        viewModel.transferOrder(order, store)
                                        Toast.makeText(context,"Pedido enviado para ${store.name}",Toast.LENGTH_LONG).show()
                                    },
                                    onPrint = { startPrintingFlow(order) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper para abrir WhatsApp
fun contactClient(context: Context, phone: String, orderId: String) {
    try {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        if (cleanPhone.isEmpty()) {
            Toast.makeText(context, "Telefone inválido ou não informado", Toast.LENGTH_SHORT).show()
            return
        }
        val ddiPhone = if (cleanPhone.startsWith("55")) cleanPhone else "55$cleanPhone"
        val message = "Olá! Referente ao seu pedido #$orderId na Drogaria Santo Antônio:"
        val url = "https://api.whatsapp.com/send?phone=$ddiPhone&text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    availableStores: List<Store>,
    onAdvance: () -> Unit,
    onCancel: (String) -> Unit,
    onTransfer: (Store) -> Unit,
    onPrint: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
    val statusColor by animateColorAsState(targetValue = when(order.status){ "Pendente"->Color(0xFFE91E63); "Entregue"->Color(0xFF4CAF50); else->MaterialTheme.colorScheme.primary })

    val items = try { Converters().fromString(order.itemsJson) } catch (e: Exception) { emptyList() }

    val paymentRaw = order.paymentMethod
    val hasChange = paymentRaw.contains("Troco", ignoreCase = true)
    val hasObs = paymentRaw.contains("Obs:", ignoreCase = true)
    val paymentDisplay = if (hasObs) paymentRaw.split("| Obs:").first().trim() else paymentRaw
    val obsDisplay = if (hasObs) paymentRaw.split("| Obs:").last().trim() else ""

    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transferir para outra Loja") },
            text = {
                if(availableStores.isEmpty()) Text("Não há outras lojas disponíveis.", color = Color.Gray)
                else LazyColumn(modifier = Modifier.heightIn(max=250.dp)) {
                    items(availableStores) { store ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(4.dp).clickable {
                                onTransfer(store)
                                showTransferDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Store, null)
                                Spacer(Modifier.width(8.dp))
                                Text(store.name, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showTransferDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Pedido") },
            text = { OutlinedTextField(value = cancelReason, onValueChange = { cancelReason = it }, label = { Text("Motivo") }, modifier=Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { if(cancelReason.isNotEmpty()){ onCancel(cancelReason); showCancelDialog = false } }, colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("Voltar") } }
        )
    }

    Card(elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column {
            Row(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = statusColor.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(order.status.uppercase(), color=statusColor, style=MaterialTheme.typography.labelSmall, modifier=Modifier.padding(horizontal=6.dp, vertical=2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(dateFormat.format(Date(order.date)), style=MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                        Text(if(order.customerName.isNotBlank()) order.customerName else "Cliente", fontWeight=FontWeight.Bold, style=MaterialTheme.typography.titleMedium)
                        if (order.customerPhone.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            // Botão WhatsApp direto no Header
                            IconButton(onClick = { contactClient(context, order.customerPhone, order.firestoreId.takeLast(4)) }, modifier=Modifier.size(24.dp)) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                            }
                        }
                    }

                    // ALERTA VISUAL NO CABEÇALHO (Se fechado)
                    if (!expanded && (hasObs || hasChange)) {
                        Row(Modifier.padding(top=4.dp), verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint=Color(0xFFE65100), modifier=Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Atenção: Ver detalhes (Obs/Troco)", color=Color(0xFFE65100), fontSize=12.sp, fontWeight=FontWeight.Bold)
                        }
                    }
                }

                IconButton(onClick = onPrint) {
                    Icon(Icons.Default.Print, "Imprimir Comprovante", tint=Color.Gray)
                }
                Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }

            if(expanded) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(16.dp)) {
                    if (order.isDelivery) {
                        Text("📍 Endereço de Entrega:", fontWeight = FontWeight.Bold)
                        Text(order.deliveryAddress, style = MaterialTheme.typography.bodyMedium)
                        
                        // Exibição explícita de Bairro e Complemento para facilitar Motoqueiro/Separador
                        if (order.addressNeighborhood.isNotEmpty() || order.addressComplement.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row {
                                if (order.addressNeighborhood.isNotEmpty()) {
                                    Surface(color = Color(0xFFE0F7FA), shape = RoundedCornerShape(4.dp)) {
                                        Text("Bairro: ${order.addressNeighborhood}", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF006064), fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (order.addressComplement.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                                        Text("Compl: ${order.addressComplement}", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // MOTOBOY ASSIGNED
                        if (order.driverName.isNotEmpty()) {
                             Spacer(Modifier.height(8.dp))
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 Icon(Icons.Default.DirectionsBike, null, tint = Color(0xFFE65100), modifier=Modifier.size(16.dp))
                                 Spacer(Modifier.width(4.dp))
                                 Text("Entregador: ${order.driverName}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                             }
                        }
                    } else {
                        Text("🏪 Retirada na Loja", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    // CARD FINANCEIRO DESTAQUE
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = if(hasChange || hasObs) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachMoney, null, modifier=Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(paymentDisplay, fontWeight=FontWeight.Bold)
                            }
                            if(obsDisplay.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text("⚠️ Obs: $obsDisplay", color=Color(0xFFD84315), fontWeight=FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ITENS
                    items.forEach { item ->
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("${item.quantity}x", fontWeight=FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(item.name, modifier=Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // AÇÕES E TOTAL
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("R$ ${String.format("%.2f", order.totalAmount)}", fontWeight=FontWeight.Bold, fontSize=18.sp, color=MaterialTheme.colorScheme.primary)

                        if(order.status != "Entregue" && order.status != "Cancelado") {
                            Row {
                                // Transferência só se estiver no início
                                if (order.status == "Pendente" || order.status == "Em Separação") {
                                    FilledTonalIconButton(onClick = { showTransferDialog=true }) { Icon(Icons.Default.SwapHoriz, "Transferir Loja", tint=Color(0xFFE65100)) }
                                    Spacer(Modifier.width(8.dp))
                                }

                                OutlinedIconButton(onClick = { showCancelDialog=true }, colors=IconButtonDefaults.outlinedIconButtonColors(contentColor=MaterialTheme.colorScheme.error)) {
                                    Icon(Icons.Default.Close, "Cancelar")
                                }

                                Spacer(Modifier.width(8.dp))

                                Button(onClick = onAdvance, colors=ButtonDefaults.buttonColors(containerColor=statusColor)) {
                                    Text(if(order.status=="Pendente") "ACEITAR" else "AVANÇAR")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}