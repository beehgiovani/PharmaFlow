package com.developersbeeh.pharmaflow.features.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProductsScreen(
    onBackClick: () -> Unit,
    viewModel: ImportProductsViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState() // 0.0 a 1.0
    val progressText by viewModel.progressText.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Launcher para arquivos JSON
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.importFile(uri)
        }
    )

    // Launcher para arquivos CSV/Excel
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.importFile(uri)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Estoque") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Alimentar Banco de Dados",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Selecione o tipo de arquivo:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• JSON: Exportação do sistema Inovafarma", style = MaterialTheme.typography.bodyMedium)
                    Text("• CSV: Planilhas padronizadas", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Text("Processando arquivo... Não feche o app.", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // BARRA DE PROGRESSO
                LinearProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                // Botão JSON (Inovafarma)
                Button(
                    onClick = { jsonPickerLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DataObject, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IMPORTAR JSON (INOVAFARMA)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão CSV
                OutlinedButton(
                    onClick = { csvPickerLauncher.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TableChart, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IMPORTAR PLANILHA CSV")
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = statusMessage,
                    textAlign = TextAlign.Center,
                    color = if (statusMessage.contains("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}