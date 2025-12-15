package com.developersbeeh.pharmaflow.data.repository

import android.net.Uri
import com.developersbeeh.pharmaflow.data.model.GlobalProduct
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val collection = firestore.collection("global_catalog")
    private val storageRef = storage.reference

    // Upload de imagem do catálogo (reaproveitando lógica)
    suspend fun uploadCatalogImage(imageUri: Uri): Result<String> {
        return try {
            val filename = "catalog_${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child("catalog_images/$filename")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveGlobalProduct(product: GlobalProduct): Result<Boolean> {
        return try {
            val docRef = if (product.id.isEmpty()) collection.document() else collection.document(product.id)
            val finalProduct = product.copy(id = docRef.id)
            docRef.set(finalProduct).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGlobalProducts(query: String): Result<List<GlobalProduct>> {
        return try {
            // Busca simples (idealmente usar Algolia ou Typesense para busca full-text em produção)
            // Aqui buscamos tudo e filtramos na memória para MVP, ou usamos whereGreaterThan se for prefixo
            val snapshot = collection.get().await()
            val all = snapshot.toObjects(GlobalProduct::class.java)

            val filtered = if (query.isEmpty()) all else all.filter {
                it.name.contains(query, ignoreCase = true) || it.ean.contains(query)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}