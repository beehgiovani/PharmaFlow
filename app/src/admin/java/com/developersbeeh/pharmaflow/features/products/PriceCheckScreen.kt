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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.developersbeeh.pharmaflow.features.products.ocr.BarcodeAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceCheckScreen(
    onBackClick: () -> Unit,
    onRegisterNewProduct: (String) -> Unit,
    viewModel: PriceCheckViewModel = hiltViewModel()
) {
    val foundProduct by viewModel.foundProduct.collectAsState()
    val notFoundError by viewModel.notFoundError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Estado para o campo de código manual
    var manualCode by remember { mutableStateOf("") }
    // Variável para guardar o último código tentado (para mostrar no erro)
    var lastTriedCode by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    // Função unificada de busca (Usada pelo ENTER do teclado e pelo Botão de Lupa)
    fun searchCode() {
        if (manualCode.isNotBlank()) {
            lastTriedCode = manualCode
            viewModel.onBarcodeScanned(manualCode)
            manualCode = "" // Limpa campo
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. CÂMERA (Fundo)
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        // Configuração idêntica à do Cadastro de Produto (funciona bem)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), BarcodeAnalyzer { barcode ->
                            // Só processa se não estiver mostrando resultado e não estiver carregando
                            if (foundProduct == null && !notFoundError && !isLoading) {
                                lastTriedCode = barcode
                                viewModel.onBarcodeScanned(barcode)
                            }
                        })
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) { e.printStackTrace() }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Câmera indisponível", color = Color.White)
            }
        }

        // 2. BARRA SUPERIOR TRANSPARENTE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Consulta de Preço",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // 3. MIRA E INPUT (Se não tiver resultado)
        if (foundProduct == null && !notFoundError) {
            // Loading Overlay
            if(isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(300.dp, 150.dp)
                        .border(2.dp, Color.Red, RoundedCornerShape(16.dp))
                )
                Text(
                    text = "Posicione o código na mira",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // --- INPUT MANUAL CORRIGIDO ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Keyboard, null, tint = Color.Gray)
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it },
                            placeholder = { Text("Digitar EAN ou Cód Interno") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { searchCode() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { searchCode() }) { // O clique do botão dispara a mesma função
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // 4. ERRO: NÃO ENCONTRADO
        if (notFoundError) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Produto não encontrado!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Código: $lastTriedCode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onRegisterNewProduct(lastTriedCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CADASTRAR AGORA")
                    }

                    androidx.compose.material3.TextButton(onClick = { viewModel.clearResult() }) {
                        Text("Tentar Novamente", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // 5. SUCESSO: PRODUTO ENCONTRADO
        foundProduct?.let { product ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Botão fechar no topo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { viewModel.clearResult() }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }

                    // Conteúdo
                    if (product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cód: ${product.ean.ifEmpty { product.internalCode }}", color = Color.Gray)
                    Text(product.category.uppercase(), color = Color.Gray, style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.height(24.dp))

                    if (product.isOnSale()) {
                        Text(
                            text = "De R$ ${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Text(
                            text = "R$ ${String.format("%.2f", product.promoPrice)}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "R$ ${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.clearResult() },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Nova Consulta")
                    }
                }
            }
        }
    }
}