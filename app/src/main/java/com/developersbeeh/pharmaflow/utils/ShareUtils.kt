package com.developersbeeh.pharmaflow.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    fun shareBitmap(context: Context, bitmap: Bitmap, text: String = "Confira essa oferta!") {
        try {
            // 1. Salvar Bitmap em arquivo temporário no cache
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // Garante que a pasta existe

            // Sobrescreve o arquivo anterior para não encher a memória
            val stream = FileOutputStream("$cachePath/offer_generated.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // 2. Pegar a URI via FileProvider (Segurança do Android)
            val newFile = File(cachePath, "offer_generated.png")
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Certifique-se que o provider está no AndroidManifest
                newFile
            )

            // 3. Criar Intent de Compartilhamento
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Permite que o Zap/Insta leia o arquivo
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "image/png"
            }

            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Oferta"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}