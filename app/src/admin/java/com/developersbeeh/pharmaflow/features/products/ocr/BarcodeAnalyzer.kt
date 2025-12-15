package com.developersbeeh.pharmaflow.features.products.ocr

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeFound: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configura formatos mais comuns em farmácias (EAN 13, EAN 8, Code 128, etc)
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_UPC_A
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.first() // Pega o primeiro que encontrar
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrEmpty()) {
                            onBarcodeFound(rawValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Log failure se necessário, mas não trava
                }
                .addOnCompleteListener {
                    // --- O PULO DO GATO ---
                    // Fecha o proxy OBRIGATORIAMENTE após terminar o processamento, sucesso ou falha
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}