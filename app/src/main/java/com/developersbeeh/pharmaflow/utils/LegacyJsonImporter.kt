package com.developersbeeh.pharmaflow.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.developersbeeh.pharmaflow.data.model.Product
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object LegacyJsonImporter {

    /**
     * Importador Otimizado para padrão INOVAFARMA
     * Formato esperado:
     * {
     *   "1": { "codigo": 123, "produto": "...", "grupo": "...", "precoVenda": 10.0, "precoPromocao": 0.0, "ean": "..." },
     *   ...
     * }
     */
    fun parseLegacyJson(context: Context, uri: Uri, storeId: String): List<Product> {
        val products = mutableListOf<Product>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Lê o arquivo inteiro para string (Cuidado com arquivos > 100MB, mas OK para texto json comum)
            val jsonString = reader.use { it.readText() }

            val root = JSONObject(jsonString)

            // O JSON do Inovafarma usa chaves numéricas ("1", "2") na raiz, então usamos o iterator de chaves
            val keys = root.keys()

            while (keys.hasNext()) {
                val key = keys.next()

                // Obtém o objeto do produto. Se falhar, pula para o próximo.
                val item = root.optJSONObject(key) ?: continue

                // 1. Extração segura de dados
                // Converte 'codigo' para String, pois pode vir como int no JSON
                val internalCode = item.optString("codigo", "").trim()

                val nameRaw = item.optString("produto", "PRODUTO SEM NOME").trim()

                val categoryRaw = item.optString("grupo", "GERAL").trim()

                // Limpeza do EAN (remove espaços e caracteres inválidos se houver)
                val eanRaw = item.optString("ean", "").replace("[^0-9]".toRegex(), "")

                val priceVenda = item.optDouble("precoVenda", 0.0)
                val pricePromo = item.optDouble("precoPromocao", 0.0)

                // 2. Lógica de Preço (Conforme solicitado)
                // Regra: "quando o preço promocional for 0,0 usar o preço de venda normal"
                // No nosso modelo Product, salvamos o preço cheio em 'price' e o promocional em 'promoPrice'.
                // O método Product.getCurrentPrice() já faz a lógica de exibir o menor.
                // Mas, garantimos que promoPrice seja 0.0 se for inválido ou igual a zero no JSON.

                val finalPromoPrice = if (pricePromo > 0.0 && pricePromo < priceVenda) {
                    pricePromo
                } else {
                    0.0
                }

                // 3. Tratamento de Nome (Limpeza extra opcional, remove asteriscos soltos etc)
                val finalName = nameRaw.replace(Regex("^\\*\\s*"), "") // Remove asterisco no início se houver

                if (finalName.isNotEmpty()) {
                    products.add(
                        Product(
                            storeId = storeId,

                            name = finalName,

                            // Preferência ao EAN, mas guarda código interno
                            ean = if (eanRaw.length > 7) eanRaw else "",
                            internalCode = internalCode,

                            description = "Importado via Inovafarma", // Descrição padrão
                            category = categoryRaw,

                            price = priceVenda,
                            promoPrice = finalPromoPrice,

                            imageUrl = "", // Importação textual não traz imagem

                            // JSON Inovafarma fornecido não tem estoque ("estoque"), então assumimos 0
                            // O gerente deve usar o "Modo Inventário" para bipar e somar quantidade.
                            stockQuantity = 0,

                            expirationDate = 0L,

                            // Regra de segurança: Começa INATIVO (false) para conferência antes de ir ao ar
                            isActive = false,
                            isFeatured = false
                        )
                    )
                }
            }
            Log.d("LegacyImporter", "Processados ${products.size} produtos com sucesso.")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("LegacyImporter", "Erro fatal na importação JSON: ${e.message}")
        }

        return products
    }
}