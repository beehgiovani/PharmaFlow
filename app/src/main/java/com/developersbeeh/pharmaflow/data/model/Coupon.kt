package com.developersbeeh.pharmaflow.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Coupon(
    @DocumentId
    val id: String = "",
    val code: String = "",          // Código que o usuário digita (ex: CLIENTE10)
    val type: String = "PERCENT",   // "PERCENT" ou "FIXED"
    val value: Double = 0.0,        // 10.0 (se percent) ou 15.00 (se fixed)
    val minPurchase: Double = 0.0,  // Compra mínima para validar

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    val storeId: String = ""        // Vazio = Global, ou ID da loja específica
)