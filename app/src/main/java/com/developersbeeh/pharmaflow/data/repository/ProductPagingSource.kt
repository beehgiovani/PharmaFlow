package com.developersbeeh.pharmaflow.data.repository

import android.util.Log

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.developersbeeh.pharmaflow.data.model.Product
import com.developersbeeh.pharmaflow.domain.sort.SortOption
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await



class ProductPagingSource(
    private val firestore: FirebaseFirestore,
    private val storeId: String,
    private val searchQuery: String,
    private val category: String,
    private val onlyActive: Boolean,
    private val filterLowStock: Boolean, // Mantido legacy OU se for usado internamente
    private val sortOption: SortOption = SortOption.DEFAULT,
    private val onlyOnSale: Boolean = false,
    private val onlyAvailable: Boolean = false // Novo, substitui filterLowStock em lógica de negócio talvez
) : PagingSource<QuerySnapshot, Product>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Product>): QuerySnapshot? {
        return null
    }

    override suspend fun load(
            params: LoadParams<QuerySnapshot>
    ): LoadResult<QuerySnapshot, Product> {
        return try {
            var query = firestore.collection("products").whereEqualTo("storeId", storeId).limit(20)

            if (onlyActive) {
                query = query.whereEqualTo("isActive", true)
            }

            if (category != "Todos") {
                query = query.whereEqualTo("category", category)
            }

            // *** FILTROS ESPECÍFICOS ***
            
            // 1. Apenas Disponíveis (Estoque > 0)
            if (onlyAvailable) {
                // whereGreaterThan exige index se combinado com outros filtros.
                // IMPORTANTE: Index Composto { storeId ASC, stockQuantity ASC, name ASC }
                query = query.whereGreaterThan("stockQuantity", 0)
            }

            // 2. Apenas Ofertas (Promo > 0)
            if (onlyOnSale) {
                query = query.whereGreaterThan("promoPrice", 0.0)
                // Index Composto { storeId ASC, promoPrice ASC, ... }
            }

            
            // *** ORDENAÇÃO ***
            // Regra do Firestore: Se tem filtro de range/desigualdade (>, <), 
            // a primeira ordenação TEM que ser nela.

            if (onlyAvailable) {
                 // Já filtramos stockQuantity > 0, então order by stockQuantity first (implícito ou explícito)
                 query = query.orderBy("stockQuantity", Query.Direction.DESCENDING) 
            } else if (onlyOnSale) {
                 query = query.orderBy("promoPrice", Query.Direction.ASCENDING)
            }

            // Se não fomos forçados a ordenar por campo de range, aplicamos a escolha do usuário
            // Mas cuidado: não podemos ordernar por Price se filtramos por PromoPrice > 0 (requires complex index or fails)
            // Vamos TENTAR respeitar a ordenação, sabendo que pode falhar sem índice.
            
            if (!onlyAvailable && !onlyOnSale) {
                 when (sortOption) {
                    SortOption.PRICE_ASC -> {
                        query = query.orderBy("price", Query.Direction.ASCENDING)
                    }
                    SortOption.PRICE_DESC -> {
                        query = query.orderBy("price", Query.Direction.DESCENDING)
                    }
                    SortOption.NAME_ASC,
                    SortOption.DEFAULT -> {
                        query = query.orderBy("name", Query.Direction.ASCENDING)
                    }
                }
            } else {
                 // Fallback secundário
                 query = query.orderBy("name", Query.Direction.ASCENDING)
            }
            
            // Lógica legada de busca (Só se não tiver outros filtros complexos pra não quebrar query)
            if (searchQuery.isNotEmpty()) {
                 if (searchQuery.all { it.isDigit() }) {
                    // Busca exata por código (EAN ou Internal)
                    query = firestore.collection("products")
                        .whereEqualTo("storeId", storeId)
                        .whereEqualTo("ean", searchQuery.trim())
                        .limit(20) // IMPORTANTE: Limit para evitar travamento
                 } else {
                     // Busca por índice de pesquisa (Case Insensitive)
                     val normalizedQuery = searchQuery.trim().lowercase()
                     query = firestore.collection("products")
                        .whereEqualTo("storeId", storeId)
                        .orderBy("searchIndex") // Ordem alfabética pelo índice
                        .startAt(normalizedQuery)
                        .endAt(normalizedQuery + "\uf8ff")
                        .limit(20) // IMPORTANTE: Limit
                 }
            }

            if (params.key != null) {
                val lastDoc = params.key!!.documents.lastOrNull()
                if (lastDoc != null) {
                    query = query.startAfter(lastDoc)
                }
            }

            val currentPage = query.get().await()
            var products = currentPage.toObjects(Product::class.java)

            // *** CLIENT SIDE RE-SORT (Safety Net) ***
            // Se o Firestore devolveu resultados mas a ordem não está perfeita devido a limitações de query composta
            if (products.isNotEmpty()) {
                products = when(sortOption) {
                     SortOption.PRICE_ASC -> products.sortedBy { it.price }
                     SortOption.PRICE_DESC -> products.sortedByDescending { it.price }
                     else -> products // Mantém ordem original (provavelmente nome ou filter field)
                }
            }

            val nextKey = if (products.isEmpty()) null else currentPage

            LoadResult.Page(data = products, prevKey = null, nextKey = nextKey)
        } catch (e: Exception) {
            Log.e("ProductPagingSource", "Error loading products", e)
            if (e is java.util.concurrent.CancellationException) throw e
            LoadResult.Error(e)
        }
    }
}
