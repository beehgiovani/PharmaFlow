package com.developersbeeh.pharmaflow.data.repository

import android.util.Log
import com.developersbeeh.pharmaflow.data.local.dao.CartDao
import com.developersbeeh.pharmaflow.data.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cartDao: CartDao
) {

    // ========================================================================
    // CRIAÇÃO E ENVIO DE PEDIDOS
    // ========================================================================

    // Salva cópia local (Room) para backup ou histórico offline básico
    suspend fun saveLocalOrder(order: Order) {
        cartDao.insertOrder(order)
    }

    // Envia o pedido para a nuvem (Firestore) na loja específica
    suspend fun sendOrderToStore(order: Order): Result<Boolean> {
        return try {
            if (order.storeId.isNotEmpty()) {
                firestore.collection("stores")
                    .document(order.storeId)
                    .collection("orders")
                    .add(order)
                    .await()
                Result.success(true)
            } else {
                Result.failure(Exception("Store ID inválido: O pedido não possui loja vinculada."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ========================================================================
    // LEITURA EM TEMPO REAL (REALTIME UPDATES)
    // ========================================================================

    // CLIENTE: Ouve todos os pedidos feitos por um usuário específico em qualquer loja
    fun getClientOrdersRealtime(userId: String): Flow<List<Order>> = callbackFlow {
        val query = firestore.collectionGroup("orders")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Se der erro de permissão ou rede, fechamos o canal
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val orders = snapshot.toObjects(Order::class.java)
                trySend(orders)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    // ADMIN: Ouve os pedidos de UMA loja específica
    fun getOrdersByStoreFlow(storeId: String): Flow<List<Order>> = callbackFlow {
        val collection = firestore.collection("stores")
            .document(storeId)
            .collection("orders")
            .orderBy("date", Query.Direction.DESCENDING)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val orders = snapshot.toObjects(Order::class.java)
                trySend(orders)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    // MOTOBOY: Ouve pedidos de VÁRIAS lojas (ZONA)
    // CORREÇÃO SÊNIOR: Filtragem híbrida (Query Simples + Filtro em Memória)
    // Isso evita problemas de índices compostos complexos no CollectionGroup.
    fun getOrdersForDeliveryZone(storeIds: List<String>): Flow<List<Order>> = callbackFlow {
        // Proteção contra crash se a lista de lojas vier vazia (motoboy sem zona)
        if (storeIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            // Passo 1: Busca TUDO das lojas da zona (query simples suportada por padrão)
            Log.d("DeliveryDebug", "Iniciando query para lojas: $storeIds")
            val query = firestore.collectionGroup("orders")
                .whereIn("storeId", storeIds)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DeliveryDebug", "ERRO FIRESTORE: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allOrders = snapshot.toObjects(Order::class.java)
                    Log.d("DeliveryDebug", "Snapshot recebido! Total de pedidos raw: ${allOrders.size}")
                    
                    allOrders.forEach { 
                        Log.d("DeliveryDebug", "Order ID: ${it.firestoreId} | Status: ${it.status} | Store: ${it.storeId}") 
                    }

                    // Passo 2: Filtro local seguro (Status e Ordenação)
                    // O motoboy só vê: "Em Separação" (para pegar) ou "Saiu para Entrega" (em curso)
                    val filteredOrders = allOrders.filter { order ->
                        order.status == "Em Separação" || order.status == "Saiu para Entrega"
                    }.sortedByDescending { it.date }

                    Log.d("DeliveryDebug", "Pedidos filtrados para UI: ${filteredOrders.size}")
                    trySend(filteredOrders)
                } else {
                    Log.d("DeliveryDebug", "Snapshot nulo.")
                    trySend(emptyList())
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e("OrderRepo", "Exception Flow Motoboy: ${e.message}")
            close(e)
        }
    }

    // ========================================================================
    // OPERAÇÕES UNITÁRIAS (CRUD & AÇÕES)
    // ========================================================================

    // Busca um pedido único para ver detalhes (Client, Motoboy, Admin)
    suspend fun getOrderById(storeId: String, orderId: String): Result<Order?> {
        return try {
            val snapshot = firestore.collection("stores")
                .document(storeId)
                .collection("orders")
                .document(orderId)
                .get()
                .await()

            val order = snapshot.toObject(Order::class.java)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Atualiza status (Avançar, Entregar) e opcionalmente atribui Motoboy
    suspend fun updateOrderStatus(
        storeId: String, 
        orderId: String, 
        newStatus: String,
        driverId: String? = null,
        driverName: String? = null
    ) {
        try {
            val updateMap = mutableMapOf<String, Any>("status" to newStatus)
            if (driverId != null) updateMap["driverId"] = driverId
            if (driverName != null) updateMap["driverName"] = driverName

            firestore.collection("stores")
                .document(storeId)
                .collection("orders")
                .document(orderId)
                .update(updateMap)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cancelar Pedido (Salva o motivo)
    suspend fun cancelOrder(storeId: String, orderId: String, reason: String) {
        try {
            val updates = mapOf(
                "status" to "Cancelado",
                "cancellationReason" to reason
            )
            firestore.collection("stores")
                .document(storeId)
                .collection("orders")
                .document(orderId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Transferir Pedido entre Lojas (TRANSAÇÃO ATÔMICA)
    // Garante que não duplica nem perde o pedido no processo
    suspend fun transferOrder(order: Order, targetStoreId: String, targetStoreName: String): Result<Boolean> {
        return try {
            firestore.runTransaction { transaction ->
                // Referência antiga (Onde o pedido está agora)
                val sourceRef = firestore.collection("stores")
                    .document(order.storeId)
                    .collection("orders")
                    .document(order.firestoreId)

                // Nova referência (Para onde vai) - Cria um ID novo
                val targetRef = firestore.collection("stores")
                    .document(targetStoreId)
                    .collection("orders")
                    .document()

                // Prepara objeto com novos dados de posse
                val movedOrder = order.copy(
                    storeId = targetStoreId,
                    storeName = targetStoreName,
                    status = "Pendente", // Volta para pendente para alertar a nova loja
                    firestoreId = targetRef.id
                )

                // Executa a troca
                transaction.set(targetRef, movedOrder) // Cria na nova
                transaction.delete(sourceRef)          // Apaga na antiga
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Estatísticas (KPIs para o Dashboard)
    // Traz pedidos dos últimos X dias para montar gráfico
    suspend fun getRecentOrders(storeId: String, daysAgo: Int = 7): Result<List<Order>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val startTime = calendar.timeInMillis

            val snapshot = firestore.collection("stores")
                .document(storeId)
                .collection("orders")
                .whereGreaterThan("date", startTime)
                .get()
                .await()

            val orders = snapshot.toObjects(Order::class.java)
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}