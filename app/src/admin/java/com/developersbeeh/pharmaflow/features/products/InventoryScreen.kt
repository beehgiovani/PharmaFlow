package com.developersbeeh.pharmaflow.features.products

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.developersbeeh.pharmaflow.features.products.ocr.BarcodeAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBackClick: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    // Estado local para entrada manual
    var manualCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    // Função local para disparo
    fun performSearch() {
        if(manualCode.isNotEmpty()) {
            viewModel.findProduct(manualCode)
            manualCode = ""
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. CÂMERA
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), BarcodeAnalyzer { barcode ->
                            viewModel.onBarcodeScanned(barcode)
                        })

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        } catch (e: Exception) { e.printStackTrace() }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Sem Câmera", color = Color.White)
            }
        }

        // 2. UI SUPERIOR E MIRA
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(50))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Inventário Rápido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, style = MaterialTheme.typography.titleMedium, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(8.dp)).padding(horizontal=8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            if (scannedProduct == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

                    // Mira visual
                    Box(
                        modifier = Modifier
                            .size(300.dp, 150.dp)
                            .border(2.dp, if(errorMsg == null) Color.Green.copy(alpha=0.6f) else Color.Red, RoundedCornerShape(12.dp))
                    ) {
                        if(isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                        else Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.Center).background(Color.Red.copy(alpha=0.8f)))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // INPUT MANUAL
                    Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            OutlinedTextField(
                                value = manualCode,
                                onValueChange = { manualCode = it },
                                placeholder = { Text("Digitar Código") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Default.Keyboard, null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            IconButton(onClick = { performSearch() }) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 3. MENSAGEM DE ERRO (Toast custom)
        if (errorMsg != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Red.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(text = errorMsg!!, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 4. OVERLAY DO PRODUTO ENCONTRADO
        if (scannedProduct != null) {
            var tempQty by remember(scannedProduct) { mutableIntStateOf(scannedProduct!!.stockQuantity) }

            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable {},
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Header
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(scannedProduct!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Atual: ${scannedProduct!!.stockQuantity}", color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.dismissProduct() }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text("Novo Estoque:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Contador
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            FilledIconButton(
                                onClick = { if (tempQty > 0) tempQty-- },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) { Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.error) }

                            Text("$tempQty", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 24.dp))

                            FilledIconButton(
                                onClick = { tempQty++ },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.updateStock(tempQty) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !isLoading
                        ) {
                            if(isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else Text("CONFIRMAR")
                        }
                    }
                }
            }
        }
    }
}