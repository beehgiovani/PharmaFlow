package com.developersbeeh.pharmaflow.features.dashboard

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developersbeeh.pharmaflow.R
import com.developersbeeh.pharmaflow.features.AdminRoutes
import com.developersbeeh.pharmaflow.ui.components.PharmaCard // Card animado
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis

// Modelo local para o grid
data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val currentStore by viewModel.currentStore.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val orderCount by viewModel.orderCount.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()

    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    // Cores dinâmicas para o grid
    val items = listOf(
        DashboardItem("Pedidos", Icons.Default.ShoppingBag, if (isDark) Color(0xFFFFCC80) else Color(0xFFFF9800), AdminRoutes.ORDERS),
        DashboardItem("Inventário", Icons.Default.Inventory, if (isDark) Color(0xFF80DEEA) else Color(0xFF00BCD4), AdminRoutes.INVENTORY),
        DashboardItem("Scanner", Icons.Default.QrCodeScanner, if (isDark) Color(0xFF82D3FF) else Color(0xFF2FB0F5), AdminRoutes.PRICE_CHECK),
        DashboardItem("Produto +", Icons.Default.AddCircle, if (isDark) Color(0xFF78DBA9) else Color(0xFF006C45), AdminRoutes.ADD_PRODUCT),
        DashboardItem("Estoque Geral", Icons.Default.Inventory, if (isDark) Color(0xFFA0CAFF) else Color(0xFF0F2E4F), AdminRoutes.STOCK_LIST),
        DashboardItem("Configurações", Icons.Default.Settings, if (isDark) Color(0xFFB0BEC5) else Color(0xFF607D8B), AdminRoutes.SETTINGS),
        DashboardItem("Equipe", Icons.Default.Security, if (isDark) Color(0xFF9FA8DA) else Color(0xFF5C6BC0), AdminRoutes.TEAM),
        DashboardItem("Marketing", Icons.Default.Campaign, if (isDark) Color(0xFFF48FB1) else Color(0xFFE91E63), AdminRoutes.MARKETING)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo Dinâmico (Loja ou Padrão)
                        if (!currentStore?.logoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(currentStore!!.logoUrl).crossfade(true).build(),
                                contentDescription = "Logo",
                                modifier = Modifier.height(30.dp).width(100.dp),
                                contentScale = ContentScale.Fit,
                                placeholder = painterResource(R.drawable.logo_pharmaflow)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.logo_pharmaflow),
                                contentDescription = "Logo",
                                modifier = Modifier.height(28.dp).width(100.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("ADMIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = currentStore?.name ?: "Selecionar Loja",
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStatistics() }) {
                        Icon(Icons.Default.Refresh, "Atualizar")
                    }
                    IconButton(onClick = {
                        viewModel.logout {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sair")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // SEÇÃO 1: GRÁFICO (Animado)
            Text(
                "Visão Geral (7 Dias)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            PharmaCard(
                delayMillis = 0,
                modifier = Modifier.height(220.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(),
                        ),
                        modelProducer = viewModel.modelProducer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 2: KPIs (Resumo Numérico)
            PharmaCard(
                delayMillis = 100,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KpiInfo("Faturamento", "R$ ${String.format("%.2f", totalSales)}")
                    KpiInfo("Pedidos", "$orderCount")

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$lowStockCount",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = if(lowStockCount>0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("Crítico", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Acesso Rápido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // SEÇÃO 3: GRID DE ATALHOS (Cascata)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(480.dp), // Altura fixa suficiente para scroll interno ou externo
                userScrollEnabled = false // Scroll controlado pela Column externa
            ) {
                itemsIndexed(items) { index, item ->
                    PharmaCard(
                        delayMillis = 150 + (index * 50),
                        onClick = { onNavigate(item.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun KpiInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f))
    }
}