package com.developersbeeh.pharmaflow.utils

import android.content.Context
import android.net.Uri
import com.developersbeeh.pharmaflow.data.model.Product
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvDataImporter {

    fun parseCsv(context: Context, uri: Uri, storeId: String): List<Product> {
        val products = mutableListOf<Product>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine() // Ignora cabeçalho

            while (reader.readLine().also { line = it } != null) {
                val tokens = line?.split(";") ?: continue

                if (tokens.size >= 3) {
                    val code = tokens[0].trim()
                    val name = tokens[1].trim()
                    val priceStr = tokens[2].trim().replace(",", ".")
                    val stockStr = if (tokens.size > 3) tokens[3].trim() else "0"
                    val category = if (tokens.size > 4) tokens[4].trim() else "Geral"

                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toIntOrNull() ?: 0

                    if (name.isNotEmpty()) {
                        products.add(
                            Product(
                                storeId = storeId,
                                name = name,
                                ean = code,
                                internalCode = code,
                                price = price,
                                stockQuantity = stock,
                                category = category,
                                description = "",
                                isFeatured = false,
                                isActive = false // <--- IMPORTA INATIVO
                            )
                        )
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return products
    }
}