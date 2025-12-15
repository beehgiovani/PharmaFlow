package com.developersbeeh.pharmaflow.utils

import android.content.Context
import com.developersbeeh.pharmaflow.data.model.Converters
import com.developersbeeh.pharmaflow.data.model.Order
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvGenerator {

    fun generateSalesCsv(context: Context, storeName: String, orders: List<Order>): File {
        val fileName = "vendas_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            val writer = FileWriter(file)

            // Cabeçalho do CSV (Padrão Excel: separado por ponto e vírgula para o Brasil)
            writer.append("ID Pedido;Data;Cliente;Status;Produto;Qtd;Preço Un.;Total Item;Total Pedido\n")

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val converter = Converters()

            for (order in orders) {
                // Ignora pedidos cancelados na exportação fiscal/estoque
                if (order.status == "Cancelado") continue

                val items = try { converter.fromString(order.itemsJson) } catch (e: Exception) { emptyList() }
                val dateStr = dateFormat.format(Date(order.date))

                for (item in items) {
                    // Linha para cada item (facilita importação de estoque)
                    writer.append("${order.firestoreId};")
                    writer.append("$dateStr;")
                    writer.append("${order.customerName};")
                    writer.append("${order.status};")
                    writer.append("${item.name};") // Ideal seria ter o EAN aqui também no futuro
                    writer.append("${item.quantity};")
                    writer.append("${String.format("%.2f", item.price).replace(".", ",")};") // Formato BR
                    writer.append("${String.format("%.2f", item.price * item.quantity).replace(".", ",")};")
                    writer.append("${String.format("%.2f", order.totalAmount).replace(".", ",")}\n")
                }
            }

            writer.flush()
            writer.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return file
    }
}