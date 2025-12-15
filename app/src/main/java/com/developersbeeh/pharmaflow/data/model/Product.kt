package com.developersbeeh.pharmaflow.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Product(
    @DocumentId
    val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val ean: String = "",
    val internalCode: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val promoPrice: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val stockQuantity: Int = 0,
    val expirationDate: Long = 0L,

    @get:PropertyName("isFeatured")
    @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    // --- NOVO CAMPO: O "Coração" da Busca ---
    // Uma string longa com todos os dados relevantes normalizados.
    // Ex: "tylenol 750mg paracetamol 789... medicamento"
    val searchIndex: String = ""
) {
    @Exclude
    fun isOnSale(): Boolean {
        return promoPrice > 0.001 && promoPrice < price // Margem segura p/ float
    }

    @Exclude
    fun getCurrentPrice(): Double {
        return if (isOnSale()) promoPrice else price
    }

    @Exclude
    fun isExpired(): Boolean {
        if (expirationDate == 0L) return false
        return System.currentTimeMillis() > expirationDate
    }

    @Exclude
    fun isExpiringSoon(): Boolean {
        if (expirationDate == 0L) return false
        val now = System.currentTimeMillis()
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        return (expirationDate - now) in 0..thirtyDaysInMillis
    }
}