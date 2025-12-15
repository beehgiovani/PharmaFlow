package com.developersbeeh.pharmaflow.utils

import java.text.Normalizer

object TextUtils {
    /**
     * Normaliza o texto para busca:
     * - Remove acentos (Não -> Nao)
     * - Converte para minúsculo
     * - Remove caracteres especiais irrelevantes
     */
    fun normalizeForSearch(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "") // Remove acentos
            .lowercase()
            .trim()
    }
}