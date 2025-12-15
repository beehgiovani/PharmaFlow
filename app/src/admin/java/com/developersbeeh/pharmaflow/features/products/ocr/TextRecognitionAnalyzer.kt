package com.developersbeeh.pharmaflow.features.products.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionAnalyzer(
    private val onTextFound: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Filtra textos muito curtos para evitar ruído
                    val text = visionText.text
                    if (text.isNotEmpty() && text.length > 3) {
                        onTextFound(text)
                    }
                }
                .addOnFailureListener {
                    // Erro silencioso ou log
                }
                .addOnCompleteListener {
                    imageProxy.close() // Importante fechar para receber o próximo frame
                }
        } else {
            imageProxy.close()
        }
    }
}