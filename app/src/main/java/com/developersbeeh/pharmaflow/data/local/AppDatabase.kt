package com.developersbeeh.pharmaflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.developersbeeh.pharmaflow.data.local.dao.CartDao
import com.developersbeeh.pharmaflow.data.local.entity.CartItem
import com.developersbeeh.pharmaflow.data.model.Converters
import com.developersbeeh.pharmaflow.data.model.Order

// Definimos as Entidades e a Versão do Banco (Agora versão 2)
@Database(
    entities = [CartItem::class, Order::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}