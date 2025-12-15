package com.developersbeeh.pharmaflow.data.model

import com.google.firebase.firestore.DocumentId

data class Store(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val whatsappNumber: String = "",
    val address: String = "",
    val pixKey: String = "",
    val deliveryFee: Double = 0.0,
    // NOVO: URL da logomarca da loja
    val logoUrl: String = "",
    val active: Boolean = true,
    // Horários de Funcionamento (ex: "08:00")
    val openingTime: String = "08:00",
    val closingTime: String = "22:00",
    val deliveryStart: String = "09:00",
    val deliveryEnd: String = "21:30"
)