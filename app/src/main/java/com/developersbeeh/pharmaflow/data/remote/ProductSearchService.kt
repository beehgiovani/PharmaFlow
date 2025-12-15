package com.developersbeeh.pharmaflow.data.remote

import android.graphics.Bitmap
import com.developersbeeh.pharmaflow.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductSearchService @Inject constructor(
    private val eanApi: EanPicturesApi,
    private val openFoodApi: ProductApiService
) {

    data class SearchResult(
        val name: String,
        val description: String,
        val imageUrl: String,
        val found: Boolean
    )

    suspend fun searchByEan(ean: String): SearchResult {
        return withContext(Dispatchers.IO) {
            // Tenta EAN PICTURES (Principal)
            try {
                // Tenta pegar a descrição JSON
                val response = eanApi.getProductDescription(ean)

                // Constrói a URL da imagem manualmente conforme o padrão da API que você passou
                // URL Padrão: http://www.eanpictures.com.br:9000/api/gtin/{EAN}
                val eanImageUrl = "http://www.eanpictures.com.br:9000/api/gtin/$ean"

                // Valida se retornou algo útil no texto
                val name = response.nome ?: response.descricao ?: ""

                // Retorna resultado "linkado" (sem baixar bytes)
                return@withContext SearchResult(
                    name = name, // Se o nome vier vazio, a UI lida com isso
                    description = response.descricao ?: "",
                    imageUrl = eanImageUrl, // Salvamos a URL direta!
                    found = true
                )

            } catch (e: Exception) {
                // Falha silenciosa no EanPictures, tenta fallback
                e.printStackTrace()
            }

            // Tenta OPEN FOOD FACTS (Fallback)
            try {
                val response = openFoodApi.getProductByEan(ean)
                if (response.status == 1 && response.product != null) {
                    return@withContext SearchResult(
                        name = response.product.productName ?: "",
                        description = "",
                        imageUrl = response.product.imageUrl ?: "", // Aqui pegamos URL HTTPS do OpenFood
                        found = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Nada encontrado
            return@withContext SearchResult("", "", "", false)
        }
    }
}