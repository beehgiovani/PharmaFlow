package com.developersbeeh.pharmaflow.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.Locale

object PixUtils {

    /**
     * Sanitiza e formata a chave para o padrão BR Code.
     * Capaz de corrigir chaves de telefone mal formatadas ou antigas.
     */
    private fun sanitizePixKey(rawKey: String): String {
        val trimmed = rawKey.trim()

        // 1. E-mail:
        if (trimmed.contains("@")) {
            return trimmed.lowercase()
        }

        // 2. Chave Aleatória (EVP - Longa e com hífens)
        if (trimmed.length > 20 && trimmed.contains("-")) {
            return trimmed
        }

        // --- ZONA DE CORREÇÃO NÚMÉRICA ---

        // Se JÁ TEM o mais (+), confiamos que está correto e retornamos
        if (trimmed.startsWith("+")) {
            return trimmed
        }

        // Remove tudo que não é número para analisar
        val nums = trimmed.replace("[^0-9]".toRegex(), "")

        return when (nums.length) {
            // Caso Clássico do Erro: 11 Digitos (DDD + 9 + Numero)
            // O sistema bancário confunde com CPF se não tiver +55.
            11 -> {
                val ddd = nums.substring(0, 2).toIntOrNull() ?: 0
                val thirdDigit = nums[2]

                // Se parece celular (DDD válido 11-99 e começa com 9), FORÇA o formato internacional
                if (ddd in 11..99 && thirdDigit == '9') {
                    "+55$nums" // CORREÇÃO AUTOMÁTICA
                } else {
                    nums // Assume CPF
                }
            }

            // Caso de Cache antigo que salvou o 55 mas sem o mais
            12, 13 -> {
                if (nums.startsWith("55")) "+$nums" else nums
            }

            // Caso telefone fixo antigo sem DDI (10 digitos)
            10 -> "+55$nums"

            // Padrão (CNPJ, CPF puro, etc)
            else -> nums
        }
    }

    fun generatePixPayload(pixKey: String, amount: Double, merchantName: String = "PHARMAFLOW", merchantCity: String = "BRASIL"): String {
        // Aplica a limpeza inteligente
        val key = sanitizePixKey(pixKey)

        val name = formatText(merchantName, 25)
        val city = formatText(merchantCity, 15)
        val amountStr = String.format(Locale.US, "%.2f", amount)

        val payload = StringBuilder()

        // 00 - Payload Format Indicator
        payload.append("000201")

        // 26 - Merchant Account Information
        val gui = "br.gov.bcb.pix"

        // Campo chave dinâmico
        val merchantBlock = "0014$gui" + "01${String.format("%02d", key.length)}$key"

        payload.append("26${String.format("%02d", merchantBlock.length)}$merchantBlock")

        // 52 - Merchant Category Code
        payload.append("52040000")

        // 53 - Transaction Currency
        payload.append("5303986")

        // 54 - Transaction Amount
        if (amount > 0) {
            payload.append("54${String.format("%02d", amountStr.length)}$amountStr")
        }

        // 58 - Country Code
        payload.append("5802BR")

        // 59 - Merchant Name
        payload.append("59${String.format("%02d", name.length)}$name")

        // 60 - Merchant City
        payload.append("60${String.format("%02d", city.length)}$city")

        // 62 - Additional Data (TxID ***)
        val field62Value = "0503***"
        payload.append("62${String.format("%02d", field62Value.length)}$field62Value")

        // 63 - CRC16 Init
        payload.append("6304")

        // Calcula CRC Final
        val crc = calculateCRC16(payload.toString())
        payload.append(crc)

        Log.d("PixUtils", "Chave Corrigida Usada: $key")

        return payload.toString()
    }

    fun generateQRCodeBitmap(content: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatText(text: String, maxLength: Int): String {
        var safe = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-zA-Z0-9 ]".toRegex(), "")
            .trim()
            .uppercase()

        if (safe.length > maxLength) safe = safe.substring(0, maxLength)
        return safe
    }

    private fun calculateCRC16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray()

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = ((b.toInt() shr (7 - i) and 1) == 1)
                val c15 = ((crc shr 15 and 1) == 1)
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        return String.format("%04X", crc and 0xFFFF).uppercase()
    }
}