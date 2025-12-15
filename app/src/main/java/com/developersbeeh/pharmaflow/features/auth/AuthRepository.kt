package com.developersbeeh.pharmaflow.features.auth

import android.util.Log
import com.developersbeeh.pharmaflow.data.model.User
import com.developersbeeh.pharmaflow.utils.GuarujaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    // --- AUTENTICAÇÃO ---

    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            val errorMsg = when(e) {
                is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
                else -> "Erro ao conectar: ${e.message}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun registerUser(
        name: String,
        email: String,
        whatsapp: String,
        neighborhood: String, // Bairro Selecionado
        address: String,      // Logradouro
        authorizedContact: Boolean,
        pass: String,
        role: String = "client"
    ): Result<Boolean> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Falha ao obter UID")

            // LÓGICA INTELIGENTE:
            // Define a loja padrão baseada no bairro escolhido no cadastro
            val automaticStoreId = GuarujaUtils.getPreferredStore(neighborhood)

            // Se for motoboy, definimos a zona de atuação baseada nessa loja inicial (pode ser mudado pelo admin)
            val driverZone = if (role.contains("driver")) {
                GuarujaUtils.getDeliveryZoneForStore(automaticStoreId)
            } else {
                ""
            }

            val newUser = User(
                id = uid,
                name = name,
                email = email,
                whatsapp = whatsapp,
                neighborhood = neighborhood,
                address = address,
                authorizedContact = authorizedContact,
                role = role,
                active = true,
                driverZone = driverZone,
                assignedStoreId = automaticStoreId, // <--- Cliente já nasce roteado!
                points = 0
            )

            firestore.collection("users").document(uid).set(newUser).await()
            Result.success(true)

        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este e-mail já está cadastrado."))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Erro ao enviar e-mail de recuperação."))
        }
    }

    // --- DADOS E FIDELIDADE ---

    suspend fun getCurrentUser(): Result<User?> {
        val uid = auth.currentUser?.uid ?: return Result.success(null)
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    // Fluxo em tempo real do Usuário
    fun getUserFlow(): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(User::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    // Atualiza Perfil (incluindo endereço e bairro para roteamento futuro)
    suspend fun updateUserProfile(whatsapp: String, address: String, neighborhood: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuário não logado"))
        return try {
            val updates = mutableMapOf<String, Any>(
                "whatsapp" to whatsapp,
                "address" to address
            )
            if (neighborhood.isNotBlank()) {
                updates["neighborhood"] = neighborhood
                // Se o bairro mudar, atualizamos a loja preferencial também?
                // Por enquanto salvamos apenas o dado bruto. A lógica de qual loja usar é dinâmica na UI.
                val preferredStore = GuarujaUtils.getPreferredStore(neighborhood)
                updates["assignedStoreId"] = preferredStore
            }
            
            firestore.collection("users").document(uid).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPoints(userId: String, pointsToAdd: Int) {
        if (userId.isEmpty() || pointsToAdd <= 0) return
        try {
            firestore.collection("users").document(userId)
                .update("points", FieldValue.increment(pointsToAdd.toLong()))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deductPoints(userId: String, pointsToDeduct: Int): Result<Boolean> {
        if (userId.isEmpty() || pointsToDeduct <= 0) return Result.success(false)
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("points") ?: 0
                if (currentPoints >= pointsToDeduct) {
                    transaction.update(userRef, "points", currentPoints - pointsToDeduct)
                    true
                } else {
                    false
                }
            }.await().let { success ->
                if (success) Result.success(true) else Result.failure(Exception("Saldo insuficiente."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- UTILITÁRIOS DE SESSÃO ---

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun logout() { auth.signOut() }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getUserRole(): String {
        val uid = getCurrentUserId() ?: return "client"
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val role = snapshot.getString("role")
            role ?: "client"
        } catch (e: Exception) {
            "client"
        }
    }
}