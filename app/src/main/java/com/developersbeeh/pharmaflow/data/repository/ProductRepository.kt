package com.developersbeeh.pharmaflow.data.repository

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.domain.sort.SortOption
import com.developersbeeh.pharmaflow.utils.TextUtils
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await


@Singleton
class ProductRepository
@Inject
constructor(
        private val firestore: FirebaseFirestore,
        private val storage: FirebaseStorage,
        @ApplicationContext private val context: Context
) {
    private val collection = firestore.collection("products")
    private val storageRef = storage.reference

    // --- SAVE COM INDEXAÇÃO AUTOMÁTICA (Batch) ---
    suspend fun saveProductsBatch(
            products: List<Product>,
            onProgress: (Int, Int) -> Unit
    ): Result<Int> {
        return try {
            val batches = products.chunked(400)
            var count = 0
            val total = products.size

            for (batchList in batches) {
                val batch = firestore.batch()

                for (product in batchList) {
                    val docId =
                            when {
                                product.internalCode.isNotEmpty() -> product.internalCode.trim()
                                product.ean.isNotEmpty() -> product.ean.trim()
                                else -> UUID.randomUUID().toString()
                            }
                    val docRef = collection.document(docId)

                    // GERA O SEARCH INDEX (Nome + EAN + Cód + Categoria - tudo minúsculo e sem
                    // acento)
                    val searchStr =
                            TextUtils.normalizeForSearch(
                                    "${product.name} ${product.ean} ${product.internalCode} ${product.category}"
                            )

                    val dataToUpdate =
                            mutableMapOf<String, Any>(
                                    "storeId" to product.storeId,
                                    "price" to product.price,
                                    "promoPrice" to product.promoPrice,
                                    "name" to product.name,
                                    "category" to product.category,
                                    "ean" to product.ean,
                                    "internalCode" to product.internalCode,
                                    "searchIndex" to searchStr // <-- SALVA NOVO CAMPO
                            )
                    batch.set(docRef, dataToUpdate, SetOptions.merge())
                }
                batch.commit().await()
                count += batchList.size
                onProgress(count, total)
            }
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // --- SAVE COM INDEXAÇÃO AUTOMÁTICA (Unitário) ---
    suspend fun saveProduct(product: Product): Result<Boolean> {
        return try {
            val docRef =
                    if (product.id.isEmpty()) collection.document()
                    else collection.document(product.id)

            // Gera Search Index antes de salvar
            val searchStr =
                    TextUtils.normalizeForSearch(
                            "${product.name} ${product.ean} ${product.internalCode} ${product.category}"
                    )

            val p = product.copy(id = docRef.id, searchIndex = searchStr)

            docRef.set(p).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Demais métodos mantidos com pequenas melhorias...

    suspend fun getProductByEan(storeId: String, rawCode: String): Result<Product?> {
        val code = rawCode.trim()
        if (code.isEmpty()) return Result.failure(Exception("Código vazio"))
        return try {
            val snapshotEan =
                    collection
                            .whereEqualTo("storeId", storeId)
                            .whereEqualTo("ean", code)
                            .limit(1)
                            .get()
                            .await()
            if (!snapshotEan.isEmpty)
                    return Result.success(snapshotEan.documents[0].toObject(Product::class.java))
            val snapshotInternal =
                    collection
                            .whereEqualTo("storeId", storeId)
                            .whereEqualTo("internalCode", code)
                            .limit(1)
                            .get()
                            .await()
            if (!snapshotInternal.isEmpty)
                    return Result.success(
                            snapshotInternal.documents[0].toObject(Product::class.java)
                    )
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPagedProducts(
        storeId: String,
        query: String,
        category: String = "Todos",
        onlyActive: Boolean = false,
        filterLowStock: Boolean = false,
        sortOption: SortOption = SortOption.DEFAULT,
        onlyOnSale: Boolean = false,
        onlyAvailable: Boolean = false
    ): Flow<PagingData<Product>> {
        return Pager(
                        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                        pagingSourceFactory = {
                            ProductPagingSource(
                                    firestore = firestore,
                                    storeId = storeId,
                                    searchQuery = query,
                                    category = category,
                                    onlyActive = onlyActive,
                                    filterLowStock = filterLowStock,
                                    sortOption = sortOption,
                                    onlyOnSale = onlyOnSale,
                                    onlyAvailable = onlyAvailable
                            )
                        }
                )
                .flow
    }

    suspend fun getFeaturedProducts(storeId: String): Result<List<Product>> {
        return try {
            val snapshot =
                    collection
                            .whereEqualTo("storeId", storeId)
                            .whereEqualTo("isActive", true)
                            .whereEqualTo("isFeatured", true)
                            .limit(20)
                            .get()
                            .await()
            Result.success(snapshot.toObjects(Product::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsByStore(
            storeId: String,
            onlyActive: Boolean = false
    ): Result<List<Product>> {
        return try {
            var query = collection.whereEqualTo("storeId", storeId)
            if (onlyActive) query = query.whereEqualTo("isActive", true)
            // Trazemos tudo para cachear (searchIndex virá junto)
            // AUMENTO DE LIMITE DE 20 PARA 1000 PARA PERMITIR MARKETING CORRETO
            val snapshot = query.limit(1000).get().await()
            Result.success(snapshot.toObjects(Product::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child("product_images/$filename")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStock(productId: String, newQuantity: Int): Result<Boolean> {
        return try {
            collection.document(productId).update("stockQuantity", newQuantity).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleProductVisibility(productId: String, isActive: Boolean): Result<Boolean> {
        return try {
            collection.document(productId).update("isActive", isActive).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            collection.document(productId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val snap = collection.limit(20).get().await()
            Result.success(snap.toObjects(Product::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductById(productId: String): Result<Product?> {
        return try {
            val snap = collection.document(productId).get().await()
            Result.success(snap.toObject(Product::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLowStockCount(storeId: String, threshold: Int = 5): Result<Long> {
        return try {
            val snap =
                    collection
                            .whereEqualTo("storeId", storeId)
                            .whereLessThan("stockQuantity", threshold)
                            .count()
                            .get(AggregateSource.SERVER)
                            .await()
            Result.success(snap.count)
        } catch (e: Exception) {
            Result.success(0)
        }
    }
}
