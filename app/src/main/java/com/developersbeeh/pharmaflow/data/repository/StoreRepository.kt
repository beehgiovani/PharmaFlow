package com.developersbeeh.pharmaflow.data.repository

import android.net.Uri
import com.developersbeeh.pharmaflow.data.model.Store
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val storesCollection = firestore.collection("stores")
    private val storageRef = storage.reference

    // --- GESTÃO DE LOJAS ---

    suspend fun getAllStores(): Result<List<Store>> {
        return try {
            val snapshot = storesCollection.get().await()
            val stores = snapshot.toObjects(Store::class.java)
            Result.success(stores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoreById(storeId: String): Result<Store?> {
        return try {
            val snapshot = storesCollection.document(storeId).get().await()
            val store = snapshot.toObject(Store::class.java)
            Result.success(store)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStore(name: String): Result<Boolean> {
        return try {
            val newStore = Store(
                name = name,
                active = true
            )
            // O ID será gerado automaticamente pelo Firestore
            storesCollection.add(newStore).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStore(store: Store): Result<Boolean> {
        return try {
            if (store.id.isNotEmpty()) {
                storesCollection.document(store.id).set(store).await()
                Result.success(true)
            } else {
                Result.failure(Exception("ID da loja inválido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStore(storeId: String): Result<Boolean> {
        return try {
            storesCollection.document(storeId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- IMAGEM DA LOJA ---

    suspend fun uploadStoreLogo(imageUri: Uri): Result<String> {
        return try {
            val filename = "store_logo_${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child("store_logos/$filename")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}