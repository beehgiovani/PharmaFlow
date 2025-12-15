package com.developersbeeh.pharmaflow.features.products

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.developersbeeh.pharmaflow.features.AdminRoutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    productId: String? = null,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val productState by viewModel.productState.collectAsState()
    val processedImageFile by viewModel.processedImageFile.collectAsState()
    val foundOnlineName by viewModel.foundOnlineName.collectAsState()

    // Estados do Formulário
    var name by remember { mutableStateOf("") }
    var ean by remember { mutableStateOf("") }
    var internalCode by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var promoPrice by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Categoria com Dropdown
    var category by remember { mutableStateOf("MEDICAMENTO") } // Valor padrão da lista
    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf(
        "CONVENIENCIA", "CORRELATOS", "ETICO", "FITOTERAPICO",
        "GENERICO", "HOMEOPATICO", "LIBERADO", "MEDICAMENTO",
        "NATURAIS", "PERFUMARIA", "SIMILAR", "SUPLEMENTOS",
        "VAREJINHO", "VITAMINA", "VITAMINAS"
    )

    var currentImageUrl by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf(0L) }
    var isFeatured by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { expirationDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(productId) {
        if (!productId.isNullOrEmpty()) viewModel.loadProduct(productId)
    }

    LaunchedEffect(productState) {
        productState?.let { product ->
            name = product.name
            ean = product.ean
            internalCode = product.internalCode
            price = product.price.toString()
            promoPrice = if (product.promoPrice > 0) product.promoPrice.toString() else ""
            stockQuantity = product.stockQuantity.toString()
            description = product.description
            category = product.category
            currentImageUrl = product.imageUrl
            expirationDate = product.expirationDate
            isFeatured = product.isFeatured
        }
    }

    LaunchedEffect(foundOnlineName) {
        foundOnlineName?.let {
            name = it
            viewModel.consumeOnlineName()
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val ocrResult by savedStateHandle?.getStateFlow<String?>("ocr_result", null)!!.collectAsState()
    LaunchedEffect(ocrResult) {
        ocrResult?.let { text ->
            if (text.isNotEmpty()) {
                name = text
                savedStateHandle?.remove<String>("ocr_result")
            }
        }
    }

    val barcodeResult by savedStateHandle?.getStateFlow<String?>("barcode_result", null)!!.collectAsState()
    LaunchedEffect(barcodeResult) {
        barcodeResult?.let { code ->
            if (code.isNotEmpty()) {
                ean = code
                viewModel.searchProductByEan(code)
                savedStateHandle?.remove<String>("barcode_result")
            }
        }
    }

    val importName by savedStateHandle?.getStateFlow<String?>("import_name", null)!!.collectAsState()
    LaunchedEffect(importName) {
        if (importName != null) {
            name = importName ?: ""
            ean = savedStateHandle?.get<String>("import_ean") ?: ""
            description = savedStateHandle?.get<String>("import_desc") ?: ""
            currentImageUrl = savedStateHandle?.get<String>("import_img") ?: ""
            savedStateHandle?.remove<String>("import_name")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) viewModel.onImageSelected(uri) }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Produto") },
            text = { Text("Tem certeza que deseja remover este produto do estoque?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        if (productId != null) {
                            viewModel.deleteProduct(productId) { success ->
                                if (success) {
                                    Toast.makeText(context, "Removido!", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                } else {
                                    Toast.makeText(context, "Erro ao remover.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId.isNullOrEmpty()) "Novo Produto" else "Editar Produto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (productId.isNullOrEmpty()) {
                OutlinedButton(
                    onClick = { navController.navigate("catalog_search") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar do Catálogo")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- ÁREA DE IMAGEM ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (processedImageFile != null) {
                    AsyncImage(model = processedImageFile, contentDescription = "Sem Fundo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)).padding(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(" Fundo Removido", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else if (currentImageUrl.isNotEmpty()) {
                    AsyncImage(model = currentImageUrl, contentDescription = "Atual", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text("Toque para foto", color = Color.Gray)
                    }
                }
                if (isLoading && processedImageFile == null && currentImageUrl.isEmpty()) CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { navController.navigate(AdminRoutes.OCR_SCAN) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ler Nome")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { navController.navigate(AdminRoutes.BARCODE_SCAN) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan EAN")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ean, onValueChange = { ean = it },
                    label = { Text("EAN (Barras)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { if (ean.isNotEmpty()) viewModel.searchProductByEan(ean) }) {
                            Icon(Icons.Default.CloudDownload, null)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = internalCode, onValueChange = { internalCode = it },
                    label = { Text("Cód. Interno") },
                    modifier = Modifier.weight(0.6f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedLabelColor = Color.Gray, unfocusedLabelColor = Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nome do Produto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- CATEGORIA (DROPDOWN NOVO) ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expandedCategory = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                )
                // Box invisível para capturar clique em todo o campo
                Box(modifier = Modifier.matchParentSize().clickable { expandedCategory = true })

                DropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Preço Normal") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = promoPrice, onValueChange = { promoPrice = it },
                    label = { Text("Oferta") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = if (promoPrice.isNotEmpty())
                        OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.error, focusedLabelColor = MaterialTheme.colorScheme.error)
                    else OutlinedTextFieldDefaults.colors()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = stockQuantity, onValueChange = { stockQuantity = it },
                label = { Text("Qtd. Estoque") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = if (expirationDate > 0) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expirationDate)) else "",
                onValueChange = {}, readOnly = true, label = { Text("Data de Validade") }, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, null) } }
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = if(isFeatured) Color(0xFFFFC107) else Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Destacar no Carrossel", fontWeight = FontWeight.Bold)
                        Text("Aparece no topo do App Cliente", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotEmpty() && price.isNotEmpty()) {
                        viewModel.saveProduct(
                            id = productId ?: "",
                            name = name,
                            ean = ean,
                            internalCode = internalCode,
                            priceStr = price,
                            promoPriceStr = promoPrice,
                            stockStr = stockQuantity,
                            description = description,
                            category = category,
                            currentImageUrl = currentImageUrl,
                            expirationDate = expirationDate,
                            isFeatured = isFeatured
                        ) { success ->
                            if (success) {
                                Toast.makeText(context, "Salvo!", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            } else {
                                Toast.makeText(context, "Erro ao salvar.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Preencha nome e preço.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (productId.isNullOrEmpty()) "CRIAR PRODUTO" else "ATUALIZAR PRODUTO")
                }
            }

            if (!productId.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXCLUIR PRODUTO")
                }
            }
        }
    }
}