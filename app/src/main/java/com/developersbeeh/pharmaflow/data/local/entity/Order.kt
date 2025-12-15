package com.developersbeeh.pharmaflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.firebase.firestore.DocumentId
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class Order(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    @DocumentId
    val firestoreId: String = "",

    val userId: String = "",
    val storeId: String = "",
    val storeName: String = "",

    val totalAmount: Double = 0.0,
    val subtotalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val pointsUsed: Int = 0,

    val paymentMethod: String = "Dinheiro",

    val isDelivery: Boolean = true,
    val deliveryAddress: String = "",
    // NOVOS CAMPOS DE ENDEREÇO
    val addressNeighborhood: String = "",
    val addressComplement: String = "",

    val date: Long = System.currentTimeMillis(),
    val status: String = "Pendente",
    val customerName: String = "",
    val customerPhone: String = "",
    val itemsJson: String = "",
    val cancellationReason: String = "",
    
    // Motoboy Attribution
    val driverId: String = "",
    val driverName: String = ""
) {
    constructor() : this(0, "", "", "", "", 0.0, 0.0, 0.0, 0.0, 0, "Dinheiro", true, "", "", "", 0L, "", "", "", "", "", "", "")
}

class Converters {
    @TypeConverter
    fun fromString(value: String): List<CartItemSummary> {
        val listType = object : TypeToken<List<CartItemSummary>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<CartItemSummary>): String {
        return Gson().toJson(list)
    }
}

// ATUALIZADO: Agora guarda a URL da imagem
data class CartItemSummary(
    val name: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val imageUrl: String = "" // <--- NOVO CAMPO
) {
    constructor() : this("", 0, 0.0, "")
}