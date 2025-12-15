package com.developersbeeh.pharmaflow.utils

import android.util.Log
import com.developersbeeh.pharmaflow.data.remote.EanPicturesApi
import com.developersbeeh.pharmaflow.data.repository.ProductRepository
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

/**
 * Utilitário para enriquecer o catálogo em massa.
 * VERSÃO DESBLOQUEADA: Salva a URL gerada e deixa o Frontend (Coil) lidar com erros.
 */
class ProductBatchEnricher @Inject constructor(
    private val productRepository: ProductRepository,
    private val eanApi: EanPicturesApi,
    @Named("InsecureClient") private val okHttpClient: OkHttpClient
) {

    // Função mantida apenas para o "Deep Repair", caso queira usar no futuro
    private fun isValidImage(url: String): Boolean {
        if (url.isEmpty()) return false
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "PharmaFlowApp/1.0").head().build()
            val response = okHttpClient.newCall(request).execute()
            val valid = response.code == 200
            response.close()
            valid
        } catch (e: Exception) {
            false // Se falhou conexão, considera inválida no Deep Repair
        }
    }

    /**
     * @param checkExistingImages Se false (Padrão), preenche vazios cegamente. Se true (Repair), testa links.
     */
    suspend fun enrichProducts(storeId: String, checkExistingImages: Boolean = false, onProgress: (String) -> Unit) {
        try {
            onProgress("Acessando banco de dados...")

            val result = productRepository.getAllProducts()
            val allProducts = result.getOrNull() ?: emptyList()

            // Filtro de elegibilidade
            val candidates = allProducts.filter { product ->
                // Filtra EANs válidos e da loja correta
                val isValidEan = product.ean.trim().length > 7
                val isMyStore = product.storeId == storeId

                if (!isValidEan || !isMyStore) return@filter false

                val needsImage = product.imageUrl.isEmpty()
                val needsDesc = product.description.isEmpty()
                // Auditoria só se for solicitado
                val auditImage = checkExistingImages && product.imageUrl.isNotEmpty()

                needsImage || needsDesc || auditImage
            }

            if (candidates.isEmpty()) {
                onProgress("Nenhum produto precisando de atualização.")
                return
            }

            onProgress("Enriquecendo ${candidates.size} produtos...")

            var updatedCount = 0

            // Chunked process
            candidates.chunked(20).forEach { batch -> // Aumentei o lote, pois não tem validação pesada agora

                batch.forEach { product ->
                    try {
                        onProgress("Salvando: ${product.name.take(15)}...")

                        var currentImage = product.imageUrl
                        var needsUpdate = false

                        // Limpeza do EAN para evitar URLs com espaços
                        val cleanEan = product.ean.trim()

                        // --- MODO 1: ENRIQUECIMENTO (Salvar Sem Validar) ---
                        // Se não tem imagem, gera a URL padrão e salva.
                        if (currentImage.isEmpty()) {
                            // Constrói a URL cega
                            currentImage = "http://www.eanpictures.com.br:9000/api/gtin/$cleanEan"
                            needsUpdate = true
                        }

                        // --- MODO 2: DEEP REPAIR (Opcional, manual) ---
                        // Apenas se o usuário clicou no botão Vermelho
                        else if (checkExistingImages) {
                            if (!isValidImage(currentImage)) {
                                currentImage = "http://www.eanpictures.com.br:9000/api/gtin/$cleanEan"
                                // Nota: Não validamos a nova também. Se a velha é ruim, tentamos a padrão.
                                needsUpdate = true
                            }
                        }

                        // --- TENTA PEGAR TEXTO (Nome/Descrição) ---
                        var newDesc = product.description
                        var newName = product.name

                        // Só chamamos a API JSON se realmente precisarmos de texto
                        // Ou se for um produto muito genérico ("PRODUTO SEM NOME")
                        if (product.description.isEmpty() || product.name.contains("PRODUTO SEM NOME", ignoreCase = true)) {
                            try {
                                val response = eanApi.getProductDescription(cleanEan)

                                if (product.description.isEmpty() && !response.descricao.isNullOrEmpty()) {
                                    newDesc = response.descricao
                                    needsUpdate = true
                                }

                                if (response.nome != null && !response.nome.isNullOrEmpty()) {
                                    // Se o nome atual é ruim, usa o da API
                                    if (product.name.length < 3 || product.name.contains("PRODUTO SEM NOME", ignoreCase = true)) {
                                        newName = response.nome
                                        needsUpdate = true
                                    }
                                }
                            } catch (e: Exception) {
                                // Se der erro na API de texto, vida que segue, já temos a imagem
                            }
                        }

                        // Commit no Firestore
                        if (needsUpdate) {
                            val updatedProduct = product.copy(
                                imageUrl = currentImage,
                                description = newDesc,
                                name = newName,
                                ean = cleanEan // Aproveita e salva o EAN sem espaços
                            )
                            productRepository.saveProduct(updatedProduct)
                            updatedCount++
                        }

                    } catch (e: Exception) {
                        Log.e("BatchEnricher", "Erro local: ${e.message}")
                    }

                    // Pequeno delay para o Firestore não reclamar
                    delay(20)
                }
            }

            onProgress("Finalizado! $updatedCount produtos processados.")

        } catch (e: Exception) {
            onProgress("Erro crítico: ${e.message}")
            e.printStackTrace()
        }
    }
}