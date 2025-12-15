package com.developersbeeh.pharmaflow.features.marketing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developersbeeh.pharmaflow.data.model.Banner
import com.developersbeeh.pharmaflow.data.model.Coupon
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.features.AdminRoutes
import com.developersbeeh.pharmaflow.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferCreatorScreen(
    onBackClick: () -> Unit,
    navController: NavController? = null,
    offerViewModel: OfferCreatorViewModel = hiltViewModel(),
    bannerViewModel: BannerManagerViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Criar Post/Oferta", "Banners do App", "Cupons")

    // Listener do Scanner
    val currentBackStackEntry = navController?.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedCodeState = savedStateHandle?.getStateFlow<String?>("barcode_result", null)?.collectAsState()
    val scannedCode = scannedCodeState?.value

    LaunchedEffect(scannedCode) {
        scannedCode?.let { code ->
            if (code.isNotEmpty() && selectedTab == 0) {
                offerViewModel.onBarcodeScanned(code)
                savedStateHandle?.remove<String>("barcode_result")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Central de Marketing") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Abas
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            when(index) {
                                0 -> Icon(Icons.Default.Campaign, null)
                                1 -> Icon(Icons.Default.ViewCarousel, null)
                                else -> Icon(Icons.Default.LocalOffer, null)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> OfferCreatorContent(offerViewModel, navController)
                1 -> BannerManagerContent(bannerViewModel)
                2 -> CouponManagerContent(bannerViewModel)
            }
        }
    }
}

// 1. ABA: CRIADOR DE OFERTAS
@Composable
fun OfferCreatorContent(viewModel: OfferCreatorViewModel, navController: NavController?) {
    val products by viewModel.products.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val templateType by viewModel.templateType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de Busca
        Surface(shadowElevation = 2.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Nome ou Código...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = if(searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { viewModel.onSearchQueryChanged("") }) { Icon(Icons.Default.Clear, null) } }
                    } else null
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { navController?.navigate(AdminRoutes.BARCODE_SCAN) },
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.White)
                }
            }
        }

        // Preview
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedProduct != null) {
                OfferPreviewCard(
                    product = selectedProduct!!,
                    storeName = viewModel.currentStoreName,
                    isStory = templateType == 1
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TouchApp, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selecione um produto para gerar", color = Color.Gray)
                }
            }
        }

        // Controles
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = templateType == 0,
                    onClick = { viewModel.setTemplate(0) },
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    icon = { Icon(Icons.Default.CropSquare, null) }
                ) { Text("Feed (1:1)") }

                SegmentedButton(
                    selected = templateType == 1,
                    onClick = { viewModel.setTemplate(1) },
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    icon = { Icon(Icons.Default.CropPortrait, null) }
                ) { Text("Story (9:16)") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedProduct != null) {
                        scope.launch {
                            Toast.makeText(context, "Gerando imagem...", Toast.LENGTH_SHORT).show()
                            val bitmap = generateHighQualityOffer(context, selectedProduct!!, viewModel.currentStoreName, templateType == 1)
                            viewModel.shareOffer(context, bitmap)
                        }
                    } else {
                        Toast.makeText(context, "Escolha um produto!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedProduct != null
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPARTILHAR OFERTA")
            }
        }

        HorizontalDivider()

        // Lista
        Text("Resultados (${products.size})", modifier = Modifier.padding(start=16.dp, top=16.dp), fontWeight = FontWeight.Bold)

        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text(if(searchQuery.isEmpty()) "Carregando catálogo..." else "Nada encontrado.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(200.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    ProductSelectionItem(
                        product = product,
                        isSelected = product.id == selectedProduct?.id,
                        onClick = { viewModel.selectProduct(product) }
                    )
                }
            }
        }
    }
}

// 2. ABA: GERENCIADOR DE BANNERS
@Composable
fun BannerManagerContent(viewModel: BannerManagerViewModel) {
    val banners by viewModel.banners.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) viewModel.uploadBanner(uri) }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Carrossel da Home", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("Adicione imagens no formato retangular (2:1).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (banners.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Nenhum banner ativo.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(1), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(banners) { banner ->
                        Card(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                            Box {
                                AsyncImage(
                                    model = banner.imageUrl,
                                    contentDescription = "Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.deleteBanner(banner) },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.White.copy(alpha=0.8f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        FloatingActionButton(
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.tertiary
        ) {
            Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.White)
        }
    }
}

// 3. ABA: GERENCIADOR DE CUPONS
@Composable
fun CouponManagerContent(viewModel: BannerManagerViewModel) {
    val coupons by viewModel.coupons.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var code by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var minPurchaseStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PERCENT") }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Criar Novo Cupom", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Código (Ex: VERAO10)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == "PERCENT", onClick = { type = "PERCENT" })
                    Text("Porcentagem (%)")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = type == "FIXED", onClick = { type = "FIXED" })
                    Text("Valor Fixo (R$)")
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it },
                        label = { Text(if(type=="PERCENT") "Desconto %" else "Valor R$") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = minPurchaseStr,
                        onValueChange = { minPurchaseStr = it },
                        label = { Text("Mínimo R$") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val v = valueStr.replace(",", ".").toDoubleOrNull()
                        val m = minPurchaseStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                        if (code.isNotEmpty() && v != null && v > 0) {
                            viewModel.createCoupon(code, type, v, m)
                            code = ""; valueStr = ""; minPurchaseStr = ""
                            Toast.makeText(context, "Cupom Criado!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Preencha corretamente.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("CRIAR CUPOM")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Cupons Ativos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(coupons) { coupon ->
                CouponItem(coupon, onDelete = { viewModel.deleteCoupon(coupon.code) })
            }
        }
    }
}

@Composable
fun CouponItem(coupon: Coupon, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(coupon.code, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                val desc = if(coupon.type == "PERCENT") "${coupon.value.toInt()}% OFF" else "R$ ${String.format("%.2f", coupon.value)} OFF"
                val regra = if(coupon.minPurchase > 0) "Mínimo: R$ ${coupon.minPurchase}" else "Sem mínimo"
                Text("$desc • $regra", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// HELPERS UI E CANVAS
@Composable
fun ProductSelectionItem(product: Product, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.LightGray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (product.ean.isNotEmpty()) Text("EAN: ${product.ean}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun OfferPreviewCard(product: Product, storeName: String, isStory: Boolean) {
    val width = 200.dp
    val height = if (isStory) 355.dp else 200.dp

    Card(
        modifier = Modifier.size(width, height),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(0.2f).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OFERTA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(storeName, color = Color.White, fontSize = 7.sp, lineHeight = 8.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }

            Column(
                modifier = Modifier.weight(0.7f).fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                AsyncImage(model = product.imageUrl, contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)

                Text(product.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)

                if (product.isOnSale()) {
                    Text("De R$ ${String.format("%.2f", product.price)}", fontSize = 8.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                    Text("Por R$ ${String.format("%.2f", product.promoPrice)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("R$ ${String.format("%.2f", product.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(0.1f).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                Text("Peça no App!", color = Color.White, fontSize = 8.sp)
            }
        }
    }
}

suspend fun generateHighQualityOffer(context: Context, product: Product, storeName: String, isStory: Boolean): Bitmap = withContext(Dispatchers.IO) {
    val width = 1080
    val height = if (isStory) 1920 else 1080

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val primaryColor = android.graphics.Color.parseColor("#BA1A1A")
    val secondaryColor = android.graphics.Color.parseColor("#0F2E4F")
    val priceColor = android.graphics.Color.parseColor("#D32F2F")
    val strikeColor = android.graphics.Color.parseColor("#757575")

    canvas.drawColor(android.graphics.Color.WHITE)

    val headerHeight = if (isStory) 350f else 300f
    val footerHeight = if (isStory) 200f else 150f
    val contentHeight = height - headerHeight - footerHeight
    val contentStartY = headerHeight

    val headerPaint = Paint().apply { color = primaryColor; isAntiAlias = true }
    canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = if(isStory) 90f else 80f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val subTitlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 45f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("OFERTA", width / 2f, headerHeight / 2f - 30f, titlePaint)
    canvas.drawText(storeName, width / 2f, headerHeight / 2f + 50f, subTitlePaint)

    val imageAreaHeight = contentHeight * 0.45f
    val imagePadding = 40f
    val imageCenterY = contentStartY + (imageAreaHeight / 2) + 20f

    if (product.imageUrl.isNotEmpty()) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(product.imageUrl).allowHardware(false).build()
            val result = loader.execute(request).drawable
            result?.let { drawable ->
                val maxImgH = (imageAreaHeight - imagePadding).toInt()
                val maxImgW = (width - 100).toInt()
                val imgSize = if (maxImgH > maxImgW) maxImgW else maxImgH
                val left = (width - imgSize) / 2
                val top = (imageCenterY - (imgSize / 2)).toInt()
                drawable.setBounds(left, top, left + imgSize, top + imgSize)
                drawable.draw(canvas)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    val textAreaStartY = contentStartY + imageAreaHeight
    val namePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = if (product.name.length > 30) 50f else 60f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val nameY = textAreaStartY + 60f
    val words = product.name.split(" ")
    val half = words.size / 2
    var line1 = product.name
    var line2 = ""
    if (product.name.length > 25 && words.size > 1) {
        line1 = words.take(half + 1).joinToString(" ")
        line2 = words.drop(half + 1).joinToString(" ")
    }
    canvas.drawText(line1, width / 2f, nameY, namePaint)
    if (line2.isNotEmpty()) {
        canvas.drawText(line2, width / 2f, nameY + 70f, namePaint)
    }

    val priceBaseY = if(line2.isNotEmpty()) nameY + 220f else nameY + 180f
    if (product.isOnSale()) {
        val oldPricePaint = Paint().apply {
            color = strikeColor
            textSize = 50f
            textAlign = Paint.Align.CENTER
            isStrikeThruText = true
            isAntiAlias = true
        }
        canvas.drawText("De R$ ${String.format("%.2f", product.price)}", width / 2f, priceBaseY - 100f, oldPricePaint)

        val newPricePaint = Paint().apply {
            color = priceColor
            textSize = 160f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("Por R$ ${String.format("%.2f", product.promoPrice)}", width / 2f, priceBaseY + 60f, newPricePaint)
    } else {
        val pricePaint = Paint().apply {
            color = secondaryColor
            textSize = 150f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("R$ ${String.format("%.2f", product.price)}", width / 2f, priceBaseY, pricePaint)
    }

    val footerPaint = Paint().apply { color = secondaryColor }
    canvas.drawRect(0f, height - footerHeight, width.toFloat(), height.toFloat(), footerPaint)
    val footerTextPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Peça no App PharmaFlow!", width / 2f, height - (footerHeight / 2) + 15f, footerTextPaint)

    return@withContext bitmap
}