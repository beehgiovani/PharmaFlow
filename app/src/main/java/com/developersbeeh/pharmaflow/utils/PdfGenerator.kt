package com.developersbeeh.pharmaflow.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.developersbeeh.pharmaflow.data.model.Order
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateOrdersReport(context: Context, storeName: String, orders: List<Order>): File {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Configuração da Página (A4: 595 x 842 pixels a 72dpi, vamos usar um pouco maior para nitidez)
        val pageWidth = 1190 // A4 dobro
        val pageHeight = 1684

        // Criação da Página 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // --- CABEÇALHO ---
        titlePaint.textSize = 40f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.color = Color.BLACK
        titlePaint.textAlign = Paint.Align.CENTER

        canvas.drawText("RELATÓRIO DE VENDAS - PHARMAFLOW", pageWidth / 2f, 80f, titlePaint)

        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Unidade: $storeName", pageWidth / 2f, 130f, paint)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Gerado em: ${dateFormat.format(Date())}", pageWidth / 2f, 170f, paint)

        // --- TABELA ---
        val startY = 250f
        var currentY = startY
        val margin = 50f

        // Cabeçalho da Tabela
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f

        val col1 = margin // Data
        val col2 = 300f   // Cliente
        val col3 = 600f   // Status
        val col4 = 900f   // Valor

        canvas.drawText("DATA", col1, currentY, paint)
        canvas.drawText("CLIENTE", col2, currentY, paint)
        canvas.drawText("STATUS", col3, currentY, paint)
        canvas.drawText("TOTAL", col4, currentY, paint)

        // Linha separadora
        currentY += 20f
        paint.strokeWidth = 2f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
        currentY += 40f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 26f

        var totalSum = 0.0

        // Lista de Itens
        for (order in orders) {
            // Se a página encher, cria nova (simplificado: para MVP vamos limitar a 1 página longa ou cortar)
            // Em produção, verificaríamos: if (currentY > pageHeight - 100) { ... }

            val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(order.date))
            val clientStr = if (order.customerName.isNotBlank()) order.customerName.take(20) else "Consumidor"
            val statusStr = order.status.uppercase()
            val totalStr = "R$ ${String.format("%.2f", order.totalAmount)}"

            canvas.drawText(dateStr, col1, currentY, paint)
            canvas.drawText(clientStr, col2, currentY, paint)

            // Cor do status
            val originalColor = paint.color
            if (statusStr == "ENTREGUE") paint.color = Color.parseColor("#4CAF50") // Verde
            else if (statusStr == "CANCELADO") paint.color = Color.RED
            else paint.color = Color.parseColor("#FF9800") // Laranja

            canvas.drawText(statusStr, col3, currentY, paint)
            paint.color = originalColor // Restaura preto

            canvas.drawText(totalStr, col4, currentY, paint)

            currentY += 50f
            totalSum += order.totalAmount
        }

        // --- RODAPÉ (TOTAIS) ---
        currentY += 20f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
        currentY += 60f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 36f
        paint.textAlign = Paint.Align.RIGHT

        canvas.drawText("TOTAL DO PERÍODO: R$ ${String.format("%.2f", totalSum)}", pageWidth - margin, currentY, paint)

        pdfDocument.finishPage(page)

        // Salvar Arquivo
        val file = File(context.cacheDir, "relatorio_vendas.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return file
    }
}