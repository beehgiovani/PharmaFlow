package com.developersbeeh.pharmaflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.developersbeeh.pharmaflow.data.local.entity.CartItem
import com.developersbeeh.pharmaflow.data.model.Order

import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    // --- CARRINHO ---

    // Retorna Flow (não precisa de suspend nem suppress)
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItem>>

    @JvmSuppressWildcards
    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getItemByProductId(productId: String): CartItem?

    @JvmSuppressWildcards
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartItem): Long

    @JvmSuppressWildcards
    @Update
    suspend fun update(item: CartItem): Int


    @JvmSuppressWildcards
    @Delete
    suspend fun delete(item: CartItem): Int

    @JvmSuppressWildcards
    @Query("DELETE FROM cart_items")
    suspend fun clearCart(): Int

    // --- HISTÓRICO DE PEDIDOS ---

    @Query("SELECT * FROM orders ORDER BY date DESC")
    fun getAllOrders(): Flow<List<Order>>

    @JvmSuppressWildcards
    @Insert
    suspend fun insertOrder(order: Order): Long
}