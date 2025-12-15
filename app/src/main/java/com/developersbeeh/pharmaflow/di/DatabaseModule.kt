package com.developersbeeh.pharmaflow.di

import android.content.Context
import androidx.room.Room
import com.developersbeeh.pharmaflow.data.local.AppDatabase
import com.developersbeeh.pharmaflow.data.local.dao.CartDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pharmaflow_db"
        )
            // Esta linha é CRUCIAL durante o desenvolvimento.
            // Se mudarmos o banco (version 1 -> 2), ela apaga o antigo e cria o novo
            // evitando o crash "IllegalStateException: Room cannot verify data integrity"
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideCartDao(database: AppDatabase): CartDao {
        return database.cartDao()
    }
}