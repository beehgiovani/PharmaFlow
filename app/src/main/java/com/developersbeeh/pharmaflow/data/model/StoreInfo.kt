package com.developersbeeh.pharmaflow.data.model

data class StoreInfo(
    val id: String = "config", // ID fixo para facilitar
    val name: String = "Minha Farmácia",
    val whatsappNumber: String = "5511999999999", // Formato internacional sem +
    val address: String = ""
)