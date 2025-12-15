package com.developersbeeh.pharmaflow.features.team

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.developersbeeh.pharmaflow.data.model.User
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import com.developersbeeh.pharmaflow.ui.components.LoadingState
import com.developersbeeh.pharmaflow.utils.GuarujaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    onBackClick: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel()
) {
    val staffList by viewModel.staffList.collectAsState()
    val pendingList by viewModel.pendingList.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Equipe, 1 = Pendentes
    var emailQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog Aprovação
    var showApproveDialog by remember { mutableStateOf<User?>(null) }
    var selectedZone by remember { mutableStateOf(GuarujaUtils.ZONE_WEST_CENTER) }

    val context = LocalContext.current

    // DIALOG: ADICIONAR GERENTE (Busca email)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; viewModel.clearSearch() },
            title = { Text("Novo Gerente") },
            text = {
                Column {
                    Text("Digite o e-mail do funcionário já cadastrado:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailQuery,
                        onValueChange = { emailQuery = it },
                        label = { Text("E-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else IconButton(onClick = { viewModel.searchUser(emailQuery) }) { Icon(Icons.Default.Search, null) }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    searchResult?.let { user ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Encontrado: ${user.name}", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    viewModel.promoteUser(user, "manager")
                                    Toast.makeText(context, "Promovido!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false; emailQuery = ""
                                }, modifier = Modifier.fillMaxWidth()) { Text("PROMOVER") }
                            }
                        }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Fechar") } }
        )
    }

    // DIALOG: APROVAR MOTOBOY E ESCOLHER ZONA
    if (showApproveDialog != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = null },
            title = { Text("Definir Zona do Motoboy") },
            text = {
                Column {
                    Text("Para aprovar ${showApproveDialog!!.name}, selecione a zona de entrega:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Radio Button: Zona A
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = selectedZone == GuarujaUtils.ZONE_WEST_CENTER, onClick = { selectedZone = GuarujaUtils.ZONE_WEST_CENTER })
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedZone == GuarujaUtils.ZONE_WEST_CENTER, onClick = { selectedZone = GuarujaUtils.ZONE_WEST_CENTER })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Zona Oeste / Centro", fontWeight = FontWeight.Bold)
                            Text("Astúrias, Pitangueiras, S. Rosa, Morrinhos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    // Radio Button: Zona B
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = selectedZone == GuarujaUtils.ZONE_EAST_PEREQUE, onClick = { selectedZone = GuarujaUtils.ZONE_EAST_PEREQUE })
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedZone == GuarujaUtils.ZONE_EAST_PEREQUE, onClick = { selectedZone = GuarujaUtils.ZONE_EAST_PEREQUE })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Zona Leste / Perequê", fontWeight = FontWeight.Bold)
                            Text("Perequê, Pernambuco, Enseada Fim", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.approveDriver(showApproveDialog!!, selectedZone)
                    showApproveDialog = null
                    Toast.makeText(context, "Motoboy Ativado!", Toast.LENGTH_SHORT).show()
                }) { Text("Confirmar Aprovação") }
            },
            dismissButton = { TextButton(onClick = { showApproveDialog = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipe") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.PersonAdd, "Adicionar Gerente", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Loja Atual") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row {
                            Text("Solicitações")
                            if(pendingList.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge { Text("${pendingList.size}") }
                            }
                        }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (isLoading && staffList.isEmpty() && pendingList.isEmpty()) {
                    LoadingState()
                } else {
                    if (selectedTab == 0) {
                        // ABA EQUIPE
                        if (staffList.isEmpty()) EmptyState("Nenhum funcionário vinculado a esta loja.")
                        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(staffList) { user -> StaffCard(user) { viewModel.removeUser(user) } }
                        }
                    } else {
                        // ABA PENDENTES (MOTOBOYS)
                        if (pendingList.isEmpty()) EmptyState("Nenhuma solicitação de motoboy pendente.")
                        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(pendingList) { user ->
                                PendingDriverCard(
                                    user = user,
                                    onApprove = { showApproveDialog = user },
                                    onReject = { viewModel.rejectDriver(user) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Card Simples (Gerentes/Balconistas)
@Composable
fun StaffCard(user: User, onRemove: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint=MaterialTheme.colorScheme.onSecondaryContainer) }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(user.name, fontWeight = FontWeight.Bold)
                    Text(user.role.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// Card com Ações (Motoboy Pendente)
@Composable
fun PendingDriverCard(user: User, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TwoWheeler, null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    Text("Bairro: ${user.neighborhood}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Recusar") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Aprovar") }
            }
        }
    }
}