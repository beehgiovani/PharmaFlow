package com.developersbeeh.pharmaflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.developersbeeh.pharmaflow.data.model.Product

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int
) {
    companion object {
        fun fromProduct(product: Product): CartItem {
            // CORREÇÃO: Usa getCurrentPrice() para garantir o valor promocional
            return CartItem(
                productId = product.id,
                name = product.name,
                price = product.getCurrentPrice(),
                imageUrl = product.imageUrl,
                quantity = 1
            )
        }
    }

    val totalPrice: Double
        get() = price * quantity
}