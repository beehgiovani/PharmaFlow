package com.developersbeeh.pharmaflow.features.cart

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.developersbeeh.pharmaflow.data.local.entity.CartItem
import com.developersbeeh.pharmaflow.data.model.Coupon
import com.developersbeeh.pharmaflow.ui.components.EmptyState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onNavigateToSuccess: () -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val items by viewModel.cartItems.collectAsState()
    val calculations by viewModel.cartCalculations.collectAsState()
    val currentStore by viewModel.currentStore.collectAsState()
    val isDelivery by viewModel.isDelivery.collectAsState()
    val address by viewModel.address.collectAsState()
    val addressNumber by viewModel.addressNumber.collectAsState()
    val addressNeighborhood by viewModel.addressNeighborhood.collectAsState()
    val addressComplement by viewModel.addressComplement.collectAsState()
    val selectedPayment by viewModel.selectedPaymentMethod.collectAsState()

    val changeAmount by viewModel.changeAmountStr.collectAsState()
    val paymentNote by viewModel.paymentNote.collectAsState()

    val userPoints by viewModel.userPoints.collectAsState()
    val isUsingPoints by viewModel.isUsingPoints.collectAsState()

    // Cupom
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()

    val orderSuccess by viewModel.orderSuccess.collectAsState()
    var showPixDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderSuccess) {
        if (orderSuccess) {
            viewModel.resetOrderSuccess()
            onNavigateToSuccess()
        }
    }

    if (showPixDialog) {
        val pixKey = currentStore?.pixKey ?: ""
        PixPaymentDialog(pixKey=pixKey, amount=calculations.total, storeName=currentStore?.name?:"Farmácia", onDismiss={showPixDialog=false}, onConfirmPaid={showPixDialog=false; viewModel.checkoutViaWhatsApp()})
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Seu Carrinho") }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = {
            if (items.isNotEmpty()) {
                CartFooter(total=calculations.total, selectedPayment=selectedPayment, onCheckout={ if (selectedPayment=="Pix") { val key=currentStore?.pixKey?:""; if(key.length>3) showPixDialog=true else Toast.makeText(context,"Sem Pix configurado",Toast.LENGTH_SHORT).show() } else viewModel.checkoutViaWhatsApp() })
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.padding(padding)) {
                EmptyState(message="Carrinho vazio", icon=Icons.Default.ShoppingCart, actionLabel="VOLTAR AS COMPRAS", onAction=onBackClick)
            }
        } else {
            LazyColumn(modifier=Modifier.padding(padding).fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
                items(items, key={it.id}) { item ->
                    SwipeableCartItem(item=item, onRemove={viewModel.removeItem(item)}, onIncrement={viewModel.increment(item)}, onDecrement={viewModel.decrement(item)})
                }
                item { HorizontalDivider(color=Color.LightGray.copy(alpha=0.5f)) }
                item { HorizontalDivider(color=Color.LightGray.copy(alpha=0.5f)) }
                item { 
                    DeliverySection(
                        isDelivery=isDelivery, 
                        address=address, 
                        addressNumber=addressNumber,
                        addressNeighborhood=addressNeighborhood,
                        addressComplement=addressComplement,
                        onModeChange={viewModel.setDeliveryMode(it)}, 
                        onAddressChange={viewModel.updateAddress(it)},
                        onNumberChange={viewModel.updateAddressNumber(it)},
                        onNeighborhoodChange={viewModel.updateAddressNeighborhood(it)},
                        onComplementChange={viewModel.updateAddressComplement(it)}
                    ) 
                }

                // NOVO: CUPOM SECTION
                item {
                    CouponSection(
                        appliedCoupon = appliedCoupon,
                        couponError = couponError,
                        onApply = { code -> viewModel.applyCoupon(code) },
                        onRemove = { viewModel.removeCoupon() }
                    )
                }

                item {
                    if (userPoints > 0) LoyaltySection(points=userPoints, isUsing=isUsingPoints, onToggle={viewModel.togglePointsUsage(it)})
                }
                item { PaymentSection(selectedPayment=selectedPayment, onPaymentSelect={viewModel.selectPaymentMethod(it)}, changeAmount=changeAmount, onChangeAmountUpdate={viewModel.updateChangeAmount(it)}, paymentNote=paymentNote, onNoteUpdate={viewModel.updatePaymentNote(it)}) }

                item { CartSummaryCard(calculations=calculations) }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// === NOVO COMPONENTE: SEÇÃO DE CUPOM ===
@Composable
fun CouponSection(
    appliedCoupon: Coupon?,
    couponError: String,
    onApply: (String) -> Unit,
    onRemove: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation=CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Cupom de Desconto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (appliedCoupon != null) {
                // Cupom Aplicado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(appliedCoupon.code, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                        val desc = if(appliedCoupon.type == "PERCENT") "${appliedCoupon.value.toInt()}%" else "R$ ${String.format("%.2f", appliedCoupon.value)}"
                        Text("Desconto de $desc aplicado", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF2E7D32))
                    }
                }
            } else {
                // Campo Inserir
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.uppercase() },
                        placeholder = { Text("Código") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onApply(text)
                            keyboard?.hide()
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onApply(text)
                        keyboard?.hide()
                    }, shape = RoundedCornerShape(8.dp)) {
                        Text("APLICAR")
                    }
                }
                if (couponError.isNotEmpty()) {
                    Text(couponError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

// Componentes da lista e pagamento
@Composable
fun SwipeableCartItem(
    item: CartItem,
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                onRemove()
                true
            } else false
        }
    )
    AnimatedVisibility(visible = !isRemoved, exit = shrinkVertically(animationSpec = tween(500)) + fadeOut()) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Red).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Default.Delete, "Remover", tint = Color.White)
                }
            },
            enableDismissFromStartToEnd = false
        ) {
            CartItemRow(item, onIncrement, onDecrement)
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if(item.imageUrl.isNotEmpty()) AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            else Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("R$ ${String.format("%.2f", item.price)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                Text(text = "${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun DeliverySection(
    isDelivery: Boolean, 
    address: String, 
    addressNumber: String,
    addressNeighborhood: String,
    addressComplement: String,
    onModeChange: (Boolean) -> Unit, 
    onAddressChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onNeighborhoodChange: (String) -> Unit,
    onComplementChange: (String) -> Unit
) {
    Column {
        Text("Tipo de Entrega", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SelectionCard(text = "Entregar", isSelected = isDelivery, onClick = { onModeChange(true) }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SelectionCard(text = "Retirar", isSelected = !isDelivery, onClick = { onModeChange(false) }, modifier = Modifier.weight(1f))
        }
        if (isDelivery) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Rua
            OutlinedTextField(
                value = address, onValueChange = onAddressChange, label = { Text("Logradouro (Rua/Av)") },
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.LocationOn, null) }, shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Número e Bairro
            Row {
                OutlinedTextField(
                    value = addressNumber, onValueChange = onNumberChange, label = { Text("Número") },
                    modifier = Modifier.weight(0.4f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = addressNeighborhood, onValueChange = onNeighborhoodChange, label = { Text("Bairro") },
                    modifier = Modifier.weight(0.6f), shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Complemento
            OutlinedTextField(
                value = addressComplement, onValueChange = onComplementChange, label = { Text("Complemento (Apto, Bloco)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun PaymentSection(
    selectedPayment: String,
    onPaymentSelect: (String) -> Unit,
    changeAmount: String,
    onChangeAmountUpdate: (String) -> Unit,
    paymentNote: String,
    onNoteUpdate: (String) -> Unit
) {
    val methods = listOf("Dinheiro", "Cartão", "Pix", "Crediário")

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Pagamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.take(2).forEach { method ->
                SelectionCard(text = method, isSelected = selectedPayment == method, onClick = { onPaymentSelect(method) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.takeLast(2).forEach { method ->
                SelectionCard(text = method, isSelected = selectedPayment == method, onClick = { onPaymentSelect(method) }, modifier = Modifier.weight(1f))
            }
        }

        // --- CAMPOS ESPECIAIS (TROCO E OBS) ---
        AnimatedVisibility(visible = selectedPayment == "Dinheiro") {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = changeAmount,
                    onValueChange = onChangeAmountUpdate,
                    label = { Text("Troco para quanto?") },
                    placeholder = { Text("Ex: 50,00 (Deixe vazio se não precisar)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Money, null, tint=MaterialTheme.colorScheme.primary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = paymentNote,
            onValueChange = onNoteUpdate,
            label = { Text("Observação de Pagamento") },
            placeholder = { Text("Ex: Dividir em 2 cartões...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SelectionCard(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(50.dp).clickable { onClick() }.border(width = if(isSelected) 2.dp else 1.dp, color = if(isSelected) MaterialTheme.colorScheme.primary else Color.LightGray, shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = if(isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else Color.White)
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal, color = if(isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
        }
    }
}

@Composable
fun LoyaltySection(points: Int, isUsing: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC107).copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = Color(0xFFFF8F00))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Você tem $points Pontos", fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                Text("Desconto: R$ ${String.format("%.2f", points/100.0)}", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = isUsing, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun CartSummaryCard(calculations: CartCalculations) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotal", color = Color.Gray); Text("R$ ${String.format("%.2f", calculations.subtotal)}", color = Color.Gray) }
        if (calculations.deliveryFee > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Entrega", color = Color.Gray); Text("+ R$ ${String.format("%.2f", calculations.deliveryFee)}", color = Color.Gray) }

        // Exibição Descontos separados
        if (calculations.discountPoints > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Desconto Fidelidade", color = MaterialTheme.colorScheme.primary); Text("- R$ ${String.format("%.2f", calculations.discountPoints)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        if (calculations.discountCoupon > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Desconto Cupom", color = MaterialTheme.colorScheme.tertiary); Text("- R$ ${String.format("%.2f", calculations.discountCoupon)}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun CartFooter(total: Double, selectedPayment: String, onCheckout: () -> Unit) {
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL A PAGAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("R$ ${String.format("%.2f", total)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) {
                Icon(if(selectedPayment=="Pix") Icons.Default.QrCode else Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if(selectedPayment=="Pix") "PAGAR COM PIX" else "ENVIAR PEDIDO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}