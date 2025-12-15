package com.developersbeeh.pharmaflow.data.repository

import com.developersbeeh.pharmaflow.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("users")

    suspend fun findUserByEmail(email: String): Result<User?> {
        return try {
            val snapshot = collection.whereEqualTo("email", email).get().await()
            if (!snapshot.isEmpty) {
                val user = snapshot.documents[0].toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Busca funcionários FIXOS da loja (Gerentes, Balconistas)
    suspend fun getStaffByStore(storeId: String): Result<List<User>> {
        return try {
            val snapshot = collection
                .whereEqualTo("assignedStoreId", storeId)
                // Excluímos drivers daqui porque eles não têm storeId fixo (agora buscaremos por zona)
                .get()
                .await()
            val staff = snapshot.toObjects(User::class.java)
            Result.success(staff)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- NOVA FUNÇÃO: Busca Motoboys ATIVOS de uma Zona ---
    suspend fun getDriversByZone(zone: String): Result<List<User>> {
        return try {
            val snapshot = collection
                .whereEqualTo("role", "driver")
                .whereEqualTo("driverZone", zone)
                .whereEqualTo("active", true)
                .get()
                .await()
            val drivers = snapshot.toObjects(User::class.java)
            Result.success(drivers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Busca motoboys PENDENTES (Geral)
    suspend fun getPendingDrivers(): Result<List<User>> {
        return try {
            val snapshot = collection.whereEqualTo("role", "driver_pending").get().await()
            val drivers = snapshot.toObjects(User::class.java)
            Result.success(drivers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Aprova o motoboy
    suspend fun approveDriver(userId: String, zone: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "role" to "driver",
                "active" to true,
                "driverZone" to zone,
                "assignedStoreId" to "" // Remove loja fixa, vira regional
            )
            collection.document(userId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectDriver(userId: String): Result<Boolean> {
        return try {
            val updates = mapOf("role" to "client", "driverZone" to "")
            collection.document(userId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: String, storeId: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "role" to newRole,
                "assignedStoreId" to storeId
            )
            collection.document(userId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeStaffAccess(userId: String): Result<Boolean> {
        return try {
            val updates = mapOf(
                "role" to "client",
                "assignedStoreId" to "",
                "driverZone" to ""
            )
            collection.document(userId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}