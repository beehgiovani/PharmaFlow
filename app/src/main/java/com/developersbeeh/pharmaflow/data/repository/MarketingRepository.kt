package com.developersbeeh.pharmaflow.data.repository

import android.net.Uri
import com.developersbeeh.pharmaflow.data.model.Banner
import com.developersbeeh.pharmaflow.data.model.Coupon
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val bannersCollection = firestore.collection("banners")
    private val couponsCollection = firestore.collection("coupons")
    private val storageRef = storage.reference.child("banners")

    // ====================================================================================
    // BANNERS (MANTIDO ESTRUTURA EXISTENTE)
    // ====================================================================================

    suspend fun uploadBannerImage(imageUri: Uri): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(filename)
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBanner(banner: Banner): Result<Boolean> {
        return try {
            bannersCollection.add(banner).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(bannerId: String): Result<Boolean> {
        return try {
            bannersCollection.document(bannerId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBanners(storeId: String): Result<List<Banner>> {
        return try {
            // Busca Global ("") OU da Loja Específica
            val snapshot = bannersCollection
                .whereIn("storeId", listOf("", storeId))
                .whereEqualTo("active", true)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            val banners = snapshot.toObjects(Banner::class.java)
            Result.success(banners)
        } catch (e: Exception) {
            // Fallback em caso de falta de índice composto
            try {
                val snapshot = bannersCollection.whereEqualTo("active", true).get().await()
                val banners = snapshot.toObjects(Banner::class.java)
                // Filtra memória se necessário
                val filtered = banners.filter { it.storeId == "" || it.storeId == storeId }
                Result.success(filtered)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    // ====================================================================================
    // CUPONS (NOVO!)
    // ====================================================================================

    // Criar novo cupom (Admin)
    suspend fun saveCoupon(coupon: Coupon): Result<Boolean> {
        return try {
            // O código deve ser único e UPPERCASE para evitar erros
            val cleanCoupon = coupon.copy(code = coupon.code.uppercase().trim())

            // Usamos o próprio Código como ID do documento para evitar duplicidade fácil
            couponsCollection.document(cleanCoupon.code).set(cleanCoupon).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listar todos os cupons (Admin - Para gerenciar)
    suspend fun getAllCoupons(): Result<List<Coupon>> {
        return try {
            val snapshot = couponsCollection.orderBy("code").get().await()
            val list = snapshot.toObjects(Coupon::class.java)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Apagar/Desativar cupom (Admin)
    suspend fun deleteCoupon(couponCode: String): Result<Boolean> {
        return try {
            couponsCollection.document(couponCode).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Validar Cupom (Cliente - Carrinho)
    // Retorna o objeto cupom se ele existir e estiver ativo
    suspend fun getCouponByCode(code: String, storeId: String): Result<Coupon?> {
        return try {
            val cleanCode = code.uppercase().trim()
            val doc = couponsCollection.document(cleanCode).get().await()

            if (doc.exists()) {
                val coupon = doc.toObject(Coupon::class.java)
                if (coupon != null) {
                    // Regras de Negócio de Validação
                    if (!coupon.isActive) {
                        return Result.failure(Exception("Cupom inativo."))
                    }
                    if (coupon.storeId.isNotEmpty() && coupon.storeId != storeId) {
                        return Result.failure(Exception("Cupom não válido para esta loja."))
                    }
                    return Result.success(coupon)
                }
            }
            Result.failure(Exception("Cupom não encontrado."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}