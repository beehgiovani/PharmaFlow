package com.developersbeeh.pharmaflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageProcessor(private val context: Context) {

    // Configuração do ML Kit para gerar o recorte automaticamente
    private val options = SubjectSegmenterOptions.Builder()
        .enableForegroundBitmap() // <--- O Pulo do Gato: Já pede o bitmap recortado
        .build()

    private val segmenter = SubjectSegmentation.getClient(options)

    /**
     * Processa a imagem:
     * 1. Identifica o objeto principal (IA).
     * 2. Recorta o fundo.
     * 3. Adiciona uma sombra suave.
     * 4. Salva em arquivo temporário.
     */
    suspend fun processProductImage(inputBitmap: Bitmap): Result<File> {
        return withContext(Dispatchers.Default) {
            try {
                val image = InputImage.fromBitmap(inputBitmap, 0)

                // 1. Processa na IA
                val result = segmenter.process(image).await()

                // 2. Obtém o recorte
                val foregroundBitmap = result.foregroundBitmap
                    ?: return@withContext Result.failure(Exception("A IA não identificou um produto claro na imagem."))

                // 3. Aplica o pós-processamento (Sombra)
                val finalBitmap = addDropShadow(foregroundBitmap)

                // 4. Salva o resultado
                val file = File(context.cacheDir, "processed_${System.currentTimeMillis()}.png")
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()

                Result.success(file)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Cria uma sombra projetada atrás do produto recortado
     * para dar efeito de estúdio e evitar que o produto pareça "flutuar".
     */
    private fun addDropShadow(originalBitmap: Bitmap): Bitmap {
        val shadowColor = Color.parseColor("#40000000") // Preto com 25% de opacidade
        val shadowRadius = 24f // Tamanho do desfoque
        val dx = 0f
        val dy = 20f // Sombra deslocada para baixo

        // Cria um canvas um pouco maior para caber a sombra
        val width = originalBitmap.width + shadowRadius.toInt() * 2
        val height = originalBitmap.height + shadowRadius.toInt() * 2

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Configura o pincel da sombra
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shadowColor
            maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
        }

        // Desenha a sombra (usando o formato alpha da imagem original)
        val alphaBitmap = originalBitmap.extractAlpha()
        canvas.drawBitmap(alphaBitmap, dx + shadowRadius, dy + shadowRadius, shadowPaint)

        // Desenha o produto original por cima
        canvas.drawBitmap(originalBitmap, shadowRadius, shadowRadius, null)

        return resultBitmap
    }
}