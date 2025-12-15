package com.developersbeeh.pharmaflow.data.repository

import com.developersbeeh.pharmaflow.data.local.dao.CartDao
import com.developersbeeh.pharmaflow.data.local.entity.CartItem
import com.developersbeeh.pharmaflow.data.model.Order

import com.developersbeeh.pharmaflow.data.model.Product
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val cartDao: CartDao
) {
    val cartItems: Flow<List<CartItem>> = cartDao.getAllCartItems()
    val orderHistory: Flow<List<Order>> = cartDao.getAllOrders() // NOVO

    suspend fun addToCart(product: Product) {
        val existingItem = cartDao.getItemByProductId(product.id)
        if (existingItem != null) {
            cartDao.update(existingItem.copy(quantity = existingItem.quantity + 1))
        } else {
            cartDao.insert(CartItem.fromProduct(product))
        }
    }

    suspend fun incrementQuantity(item: CartItem) {
        cartDao.update(item.copy(quantity = item.quantity + 1))
    }

    suspend fun decrementQuantity(item: CartItem) {
        if (item.quantity > 1) {
            cartDao.update(item.copy(quantity = item.quantity - 1))
        } else {
            cartDao.delete(item)
        }
    }

    suspend fun deleteItem(item: CartItem) {
        cartDao.delete(item)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }


    // NOVO: Salvar Histórico
    suspend fun saveOrder(order: Order) {
        cartDao.insertOrder(order)
    }
}