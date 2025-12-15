package com.developersbeeh.pharmaflow.features.cart

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developersbeeh.pharmaflow.data.local.SessionManager
import com.developersbeeh.pharmaflow.data.local.entity.CartItem
import com.developersbeeh.pharmaflow.data.model.CartItemSummary
import com.developersbeeh.pharmaflow.data.model.Coupon
import com.developersbeeh.pharmaflow.data.model.Order
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.data.model.Store
import com.developersbeeh.pharmaflow.data.repository.CartRepository
import com.developersbeeh.pharmaflow.data.repository.MarketingRepository
import com.developersbeeh.pharmaflow.data.repository.OrderRepository
import com.developersbeeh.pharmaflow.features.auth.AuthRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

data class CartCalculations(
    val subtotal: Double = 0.0,
    val discountPoints: Double = 0.0,
    val discountCoupon: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val marketingRepository: MarketingRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentStore = sessionManager.currentStore

    // UI State
    private val _userPoints = MutableStateFlow(0)
    val userPoints = _userPoints.asStateFlow()

    private val _isUsingPoints = MutableStateFlow(false)
    val isUsingPoints = _isUsingPoints.asStateFlow()

    // CUPOM
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon = _appliedCoupon.asStateFlow()
    private val _couponError = MutableStateFlow("")
    val couponError = _couponError.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("Dinheiro")
    val selectedPaymentMethod = _selectedPaymentMethod.asStateFlow()
    private val _paymentNote = MutableStateFlow("")
    val paymentNote = _paymentNote.asStateFlow()
    private val _changeAmountStr = MutableStateFlow("")
    val changeAmountStr = _changeAmountStr.asStateFlow()
    private val _isDelivery = MutableStateFlow(true)
    val isDelivery = _isDelivery.asStateFlow()
    private val _address = MutableStateFlow("")
    val address = _address.asStateFlow()
    // NOVOS CAMPOS DE ENDEREÇO
    private val _addressNumber = MutableStateFlow("")
    val addressNumber = _addressNumber.asStateFlow()
    private val _addressNeighborhood = MutableStateFlow("")
    val addressNeighborhood = _addressNeighborhood.asStateFlow()
    private val _addressComplement = MutableStateFlow("")
    val addressComplement = _addressComplement.asStateFlow()
    private val _orderSuccess = MutableStateFlow(false)
    val orderSuccess = _orderSuccess.asStateFlow()

    init { loadUserData() }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser().getOrNull()
            if (user != null) {
                _userPoints.value = user.points
                if (user.address.isNotEmpty()) _address.value = user.address
            }
        }
    }

    // Ações
    fun togglePointsUsage(use: Boolean) { _isUsingPoints.value = use }
    fun selectPaymentMethod(method: String) { _selectedPaymentMethod.value = method }
    fun setDeliveryMode(delivery: Boolean) { _isDelivery.value = delivery }
    fun updateAddress(newAddress: String) { _address.value = newAddress }
    fun updateAddressNumber(value: String) { _addressNumber.value = value }
    fun updateAddressNeighborhood(value: String) { _addressNeighborhood.value = value }
    fun updateAddressComplement(value: String) { _addressComplement.value = value }
    fun updatePaymentNote(note: String) { _paymentNote.value = note }
    fun updateChangeAmount(value: String) { _changeAmountStr.value = value }
    fun resetOrderSuccess() { _orderSuccess.value = false }

    // CUPOM ACTIONS
    fun applyCoupon(code: String) {
        if (code.isEmpty()) return
        val storeId = sessionManager.getCurrentStoreId()
        viewModelScope.launch {
            _couponError.value = ""
            val result = marketingRepository.getCouponByCode(code, storeId)

            if (result.isSuccess) {
                val coupon = result.getOrNull()
                _appliedCoupon.value = coupon
            } else {
                _appliedCoupon.value = null
                _couponError.value = result.exceptionOrNull()?.message ?: "Cupom inválido."
            }
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponError.value = ""
    }

    val cartItems = cartRepository.cartItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CÁLCULO MESTRE ---
    @Suppress("UNCHECKED_CAST")
    val cartCalculations: StateFlow<CartCalculations> = combine(
        listOf(
            cartRepository.cartItems, // 0
            _isUsingPoints,           // 1
            _userPoints,              // 2
            _isDelivery,              // 3
            currentStore,             // 4
            _appliedCoupon            // 5
        )
    ) { args ->

        val items = args[0] as List<CartItem>
        val usePoints = args[1] as Boolean
        val points = args[2] as Int
        val isDeliv = args[3] as Boolean
        val store = args[4] as? Store
        val coupon = args[5] as? Coupon

        val subtotal = items.sumOf { it.totalPrice }
        val discountP = if (usePoints) (points / 100.0) else 0.0
        val fee = if (isDeliv) (store?.deliveryFee ?: 0.0) else 0.0

        // Regra Cupom
        var discountC = 0.0
        if (coupon != null) {
            if (subtotal >= coupon.minPurchase) {
                discountC = if (coupon.type == "PERCENT") {
                    subtotal * (coupon.value / 100.0)
                } else {
                    coupon.value
                }
            }
        }

        // Soma descontos mas não deixa total negativo
        val totalDiscount = discountP + discountC
        val productsTotal = (subtotal - totalDiscount).coerceAtLeast(0.0)
        val finalTotal = productsTotal + fee

        CartCalculations(subtotal, discountP, discountC, fee, finalTotal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CartCalculations())

    fun addToCart(product: Product) { viewModelScope.launch { cartRepository.addToCart(product) } }
    fun increment(item: CartItem) { viewModelScope.launch { cartRepository.incrementQuantity(item) } }
    fun decrement(item: CartItem) { viewModelScope.launch { cartRepository.decrementQuantity(item) } }
    fun removeItem(item: CartItem) { viewModelScope.launch { cartRepository.deleteItem(item) } }

    fun checkoutViaWhatsApp() {
        viewModelScope.launch {
            val items = cartItems.value
            if (items.isEmpty()) return@launch
            if (_isDelivery.value && _address.value.length < 5) {
                Toast.makeText(context, "Informe endereço", Toast.LENGTH_LONG).show()
                return@launch
            }

            val calcs = cartCalculations.value
            val usePoints = isUsingPoints.value
            val payment = _selectedPaymentMethod.value
            val isDeliv = _isDelivery.value
            val addressText = _address.value
            val obsPagamento = _paymentNote.value
            val trocoStr = _changeAmountStr.value
            val appliedCup = _appliedCoupon.value

            val store = sessionManager.currentStore.value
            
            // VALIDAÇÃO DE HORÁRIO
            if (store != null) {
                // Import manual se necessário, mas o IDE resolve
                if (!com.developersbeeh.pharmaflow.utils.StoreStatusHelper.isStoreOpen(store)) {
                     Toast.makeText(context, "Loja Fechada! 🕒\nHorário: ${store.openingTime} às ${store.closingTime}", Toast.LENGTH_LONG).show()
                     return@launch
                }
                
                // --- NOVA VALIDAÇÃO DE HORÁRIO DE ENTREGA COM ZONA ---
                val deliveryBairro = if(isDeliv) _addressNeighborhood.value else ""
                if (isDeliv && !com.developersbeeh.pharmaflow.utils.StoreStatusHelper.isDeliveryTime(store, deliveryBairro)) {
                    val offset = if(deliveryBairro.isNotEmpty()) com.developersbeeh.pharmaflow.utils.GuarujaUtils.getClosingOffsetMinutes(deliveryBairro) else 0
                    val msg = if (offset > 0) {
                         "Entregas para $deliveryBairro encerram $offset min mais cedo.\nLoja fechada para esta região."
                    } else {
                         "Fora do horário de entrega!\n${store.deliveryStart} às ${store.deliveryEnd}"
                    }
                    Toast.makeText(context, "⚠️ $msg", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

            val phone = store?.whatsappNumber ?: ""
            val storeName = store?.name ?: "Farmácia"
            val pixKey = store?.pixKey ?: ""

            if (phone.isEmpty()) { Toast.makeText(context, "Loja sem WhatsApp.", Toast.LENGTH_SHORT).show(); return@launch }

            val currentUser = authRepository.getCurrentUser().getOrNull()
            val userId = currentUser?.id ?: ""
            val userName = currentUser?.name ?: "Cliente"

            if (userId.isNotEmpty() && isDeliv && addressText.isNotEmpty()) {
                // ZONE VALIDATION
                val targetStoreId = com.developersbeeh.pharmaflow.utils.GuarujaUtils.getPreferredStore(_addressNeighborhood.value)
                val currentStoreId = sessionManager.getCurrentStoreId()

                if (targetStoreId != currentStoreId) {
                    Toast.makeText(context, "⚠️ Endereço fora da área desta loja!\nPor favor, troque a loja no topo da tela inicial.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                if (addressText != currentUser?.address || _addressNeighborhood.value != currentUser?.neighborhood) {
                    authRepository.updateUserProfile(currentUser?.whatsapp ?: "", addressText, _addressNeighborhood.value)
                }
            }

            // Descrição Pagamento Banco
            var payDb = payment
            if (payment == "Dinheiro" && trocoStr.isNotEmpty()) payDb += " (Troco p/ R$$trocoStr)"
            if (obsPagamento.isNotEmpty()) payDb += " | Obs: $obsPagamento"

            // Pontos
            var ptsDeduct = 0
            if (userId.isNotEmpty() && usePoints && calcs.discountPoints > 0) {
                ptsDeduct = (calcs.discountPoints * 100).toInt()
                val result = authRepository.deductPoints(userId, ptsDeduct)
                if (result.isFailure) { Toast.makeText(context, "Saldo insuficiente", Toast.LENGTH_SHORT).show(); return@launch }
            }

            val summaries = items.map { CartItemSummary(it.name, it.quantity, it.price, it.imageUrl) }
            
            // Constrói string completa para exibição e mensagem
            val fullAddress = if(isDeliv) "$addressText, $trocoStr (Nº ${_addressNumber.value}) - ${_addressNeighborhood.value}. Compl: ${_addressComplement.value}" else "Retirada"
            // Oops, trocoStr ali em cima foi erro de digitação, corrigindo para usar variaveis certas
            val displayAddress = if(isDeliv) "$addressText, ${_addressNumber.value} - ${_addressNeighborhood.value}. ${_addressComplement.value}" else "Retirada"

            val newOrder = Order(
                userId = userId, storeId = sessionManager.getCurrentStoreId(), storeName = storeName,
                totalAmount = calcs.total, subtotalAmount = calcs.subtotal,
                discountAmount = calcs.discountPoints + calcs.discountCoupon,
                deliveryFee = calcs.deliveryFee, pointsUsed = ptsDeduct, paymentMethod = payDb,
                isDelivery = isDeliv, 
                deliveryAddress = displayAddress,
                addressNeighborhood = _addressNeighborhood.value,
                addressComplement = _addressComplement.value,
                itemsJson = Gson().toJson(summaries), status = "Pendente", customerName = userName, customerPhone = currentUser?.whatsapp ?: ""
            )

            orderRepository.saveLocalOrder(newOrder)
            orderRepository.sendOrderToStore(newOrder)

            val msg = buildString {
                append("*PEDIDO PharmaFlow* 💊\n")
                append("👤 $userName\n")
                append("----------------\n")
                items.forEach { append("▪️ ${it.quantity}x ${it.name}\n") }

                items.forEach { append("▪️ ${it.quantity}x ${it.name}\n") }
                if (isDeliv) append("\n📍 Entregar: $addressText, ${_addressNumber.value}\nBairro: ${_addressNeighborhood.value}\nCompl: ${_addressComplement.value}")
                else append("\n🏪 Retirada na Loja")

                append("\n\n📦 Subtotal: R$ ${String.format("%.2f", calcs.subtotal)}")
                if (calcs.deliveryFee > 0) append("\n🛵 Frete: +R$ ${String.format("%.2f", calcs.deliveryFee)}")
                if (calcs.discountPoints > 0) append("\n⭐ Pontos: -R$ ${String.format("%.2f", calcs.discountPoints)}")
                if (calcs.discountCoupon > 0) append("\n🎟 Cupom (${appliedCup?.code}): -R$ ${String.format("%.2f", calcs.discountCoupon)}")

                append("\n💰 *TOTAL: R$ ${String.format("%.2f", calcs.total)}*")

                append("\n💳 $payment")
                if(payment == "Dinheiro" && trocoStr.isNotEmpty()) append(" (Troco p/ R$$trocoStr)")
                if(payment == "Pix" && pixKey.isNotEmpty()) append("\n\n💠 Chave: $pixKey")
                if(obsPagamento.isNotEmpty()) append("\n📝 Obs: $obsPagamento")
            }

            try {
                val url = "https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(msg, "UTF-8")}"

                // --- CORREÇÃO DO ERRO ---
                // Configurando a intent corretamente com FLAG_ACTIVITY_NEW_TASK
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(intent)

                cartRepository.clearCart()
                _isUsingPoints.value = false; _appliedCoupon.value = null; _orderSuccess.value = true
                _changeAmountStr.value = ""; _paymentNote.value = ""

            } catch (e: Exception) {
                // Caso o WhatsApp não esteja instalado, evitar o crash com tratamento de erro
                // É melhor avisar na tela principal
                e.printStackTrace()
            }
        }
    }
}