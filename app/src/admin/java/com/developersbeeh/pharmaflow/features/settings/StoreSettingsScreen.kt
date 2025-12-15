package com.developersbeeh.pharmaflow.features.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

// --- CORREÇÃO DE CRASH: MÁSCARA TELEFONE SEGURA ---
class PhoneMaskTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Formato esperado na tela: (XX) XXXXX-XXXX
        val trimmed = if (text.text.length >= 11) text.text.substring(0, 11) else text.text
        var out = ""
        for (i in trimmed.indices) {
            if (i == 0) out += "("
            if (i == 2) out += ") "
            if (i == 7) out += "-"
            out += trimmed[i]
        }

        val numberOffsetTranslator = object : OffsetMapping {
            // Mapeia do texto original (puro) para o texto visual (com mascara)
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 2) return offset + 1 // Pula (
                if (offset <= 7) return offset + 3 // Pula )_
                if (offset <= 11) return offset + 4 // Pula -
                return out.length
            }

            // Mapeia do texto visual (onde o cursor está) para o texto original
            override fun transformedToOriginal(offset: Int): Int {
                if (text.isEmpty()) return 0

                val calculated = when {
                    offset <= 1 -> 0
                    offset <= 5 -> offset - 1
                    offset <= 10 -> offset - 3
                    else -> offset - 4
                }

                // *** CORREÇÃO CRÍTICA ***
                // Garante que o índice NUNCA ultrapasse o tamanho da string original
                // Isso previne o 'OffsetMapping... invalid mapping' crash.
                return calculated.coerceIn(0, text.length)
            }
        }

        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

class TimeMaskTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Formato: XX:XX
        val trimmed = if (text.text.length >= 4) text.text.substring(0, 4) else text.text
        var out = ""
        for (i in trimmed.indices) {
            if (i == 2) out += ":"
            out += trimmed[i]
        }

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return out.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                 if (text.isEmpty()) return 0
                val calculated = when {
                    offset <= 2 -> offset
                    else -> offset - 1
                }
                return calculated.coerceIn(0, text.length) // Safe
            }
        }
        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: StoreSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentInfo by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEnriching by viewModel.isEnriching.collectAsState()
    val enrichProgress by viewModel.enrichProgress.collectAsState()

    var name by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pixKey by remember { mutableStateOf("") }
    var deliveryFee by remember { mutableStateOf("") }

    var openingTime by remember { mutableStateOf("08:00") }
    var closingTime by remember { mutableStateOf("22:00") }
    var deliveryStart by remember { mutableStateOf("09:00") }
    var deliveryEnd by remember { mutableStateOf("21:30") }
    var storeActive by remember { mutableStateOf(true) }
    
    val timeMask = remember { TimeMaskTransformation() } // Add this!

    var currentLogoUrl by remember { mutableStateOf("") }
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }

    var pixKeyType by remember { mutableStateOf("CPF") }
    var expandedType by remember { mutableStateOf(false) }

    val keyTypes = listOf("CPF", "CNPJ", "Celular", "E-mail", "Aleatória")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedLogoUri = uri }
    )

    // CARREGAR DADOS EXISTENTES
    LaunchedEffect(currentInfo) {
        if (currentInfo.name.isNotEmpty()) name = currentInfo.name
        if (currentInfo.whatsappNumber.isNotEmpty()) whatsapp = currentInfo.whatsappNumber
        if (currentInfo.address.isNotEmpty()) address = currentInfo.address

        openingTime = currentInfo.openingTime.replace(":", "")
        closingTime = currentInfo.closingTime.replace(":", "")
        deliveryStart = currentInfo.deliveryStart.replace(":", "")
        deliveryEnd = currentInfo.deliveryEnd.replace(":", "")
        storeActive = currentInfo.active

        currentLogoUrl = currentInfo.logoUrl
        deliveryFee = if(currentInfo.deliveryFee > 0) String.format("%.2f", currentInfo.deliveryFee) else ""

        // Lógica para detectar tipo de chave e preparar o campo visual
        val dbKey = currentInfo.pixKey
        if (dbKey.startsWith("+55")) {
            pixKeyType = "Celular"
            // Remove o +55 para que a máscara visual (XX) XXXX... funcione corretamente
            pixKey = dbKey.replace("+55", "").trim()
        } else if (dbKey.contains("@")) {
            pixKeyType = "E-mail"
            pixKey = dbKey
        } else if (dbKey.length == 14 && dbKey.all { it.isDigit() }) {
            pixKeyType = "CNPJ"
            pixKey = dbKey
        } else if (dbKey.length > 20) {
            pixKeyType = "Aleatória"
            pixKey = dbKey
        } else {
            // Default CPF ou celular mal formatado
            pixKeyType = if (dbKey.length <= 11) "CPF" else "Aleatória"
            pixKey = dbKey
        }
    }

    if (isEnriching) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Trabalhando...") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = enrichProgress, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações da Loja") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seletor Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.3f))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedLogoUri != null) {
                    AsyncImage(model = selectedLogoUri, contentDescription = "Novo Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (currentLogoUrl.isNotEmpty()) {
                    AsyncImage(model = currentLogoUrl, contentDescription = "Logo Atual", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                }
            }
            Text(text = "Alterar logo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // Formulário
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da Loja") }, leadingIcon = { Icon(Icons.Default.Store, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp (Pedidos)") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço Completo") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("Taxa de Entrega (R$)") }, leadingIcon = { Icon(Icons.Default.LocalShipping, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)

            Spacer(modifier = Modifier.height(24.dp))

            // --- SEÇÃO PIX (Com tratamento para Crash) ---
            Text("Chave Pix", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Box(modifier = Modifier.weight(0.4f).padding(end = 8.dp)) {
                    OutlinedTextField(
                        value = pixKeyType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { IconButton(onClick = { expandedType = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        keyTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = {
                                pixKeyType = type
                                expandedType = false
                                // Limpa campo ao trocar tipo para evitar confusão de máscara
                                pixKey = ""
                            })
                        }
                    }
                }

                // Aplica máscara APENAS se for celular
                val transformation = if (pixKeyType == "Celular") remember { PhoneMaskTransformation() } else VisualTransformation.None
                val kType = if (pixKeyType == "E-mail" || pixKeyType == "Aleatória") KeyboardType.Text else KeyboardType.Number

                OutlinedTextField(
                    value = pixKey,
                    onValueChange = { input ->
                        if (pixKeyType == "Celular") {
                            // Limita a 11 digitos NUMÉRICOS para caber na máscara e não bugar
                            val cleanInput = input.filter { it.isDigit() }
                            if (cleanInput.length <= 11) {
                                pixKey = cleanInput
                            }
                        } else {
                            pixKey = input
                        }
                    },
                    label = { Text(if(pixKeyType == "Celular") "DDD + Número" else "Chave") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = kType),
                    visualTransformation = transformation
                )
            }
            if(pixKeyType == "Celular") {
                Text("Formato automático (XX) XXXXX-XXXX. Salvará com +55.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top=4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // --- CONFIGURAÇÃO DE HORÁRIOS ---
            Text("Horário de Funcionamento", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = openingTime,
                    onValueChange = { if(it.length <= 4 && it.all { c -> c.isDigit() }) openingTime = it },
                    label = { Text("Abre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = timeMask,
                    placeholder = { Text("0800") }
                )
                OutlinedTextField(
                    value = closingTime,
                    onValueChange = { if(it.length <= 4 && it.all { c -> c.isDigit() }) closingTime = it },
                    label = { Text("Fecha") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = timeMask,
                    placeholder = { Text("2200") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Horário de Entregas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = deliveryStart,
                    onValueChange = { if(it.length <= 4 && it.all { c -> c.isDigit() }) deliveryStart = it },
                    label = { Text("Início") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = timeMask,
                    placeholder = { Text("0900") }
                )
                OutlinedTextField(
                    value = deliveryEnd,
                    onValueChange = { if(it.length <= 4 && it.all { c -> c.isDigit() }) deliveryEnd = it },
                    label = { Text("Fim") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = timeMask,
                    placeholder = { Text("2130") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // --- CONTROLE DE LOJA ATIVA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (storeActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .clickable { storeActive = !storeActive },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (storeActive) "LOJA ABERTA" else "LOJA FECHADA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (storeActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (storeActive) "Recebendo pedidos normalmente" else "Pedidos bloqueados temporariamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (storeActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                androidx.compose.material3.Switch(
                    checked = storeActive,
                    onCheckedChange = { storeActive = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.saveSettings(
                        name, whatsapp, address, pixKey, pixKeyType, deliveryFee, selectedLogoUri,
                        openingTime, closingTime, deliveryStart, deliveryEnd, storeActive
                    ) { success ->
                        if (success) {
                            Toast.makeText(context, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        } else {
                            Toast.makeText(context, "Erro ao salvar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White) else Text("SALVAR CONFIGURAÇÕES")
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Manutenção", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.startEnrichment() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("ENRIQUECER (PADRÃO)")
            }
            Text("Busca imagens para produtos vazios.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startDeepRepair() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Build, null)
                Spacer(Modifier.width(8.dp))
                Text("REPARAR IMAGENS")
            }
            Text("Lento: Valida links e corrige imagens quebradas.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}