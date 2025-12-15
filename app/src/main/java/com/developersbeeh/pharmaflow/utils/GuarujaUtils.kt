package com.developersbeeh.pharmaflow.utils

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilitário responsável pela inteligência logística do Guarujá.
 * Mapeia Bairros -> Zonas -> Lojas.
 */
object GuarujaUtils {

    // IDs das Lojas (Devem coincidir com o Firestore)
    const val STORE_ID_ASTURIAS = "loja_01_asturias"
    const val STORE_ID_MORRINHOS = "loja_02_morrinhos"
    const val STORE_ID_STAROSA = "loja_03_starosa"
    const val STORE_ID_PEREQUE1 = "loja_04_pereque1"
    const val STORE_ID_PEREQUE2 = "loja_05_pereque2"

    // Zonas de Motoboy
    const val ZONE_WEST_CENTER = "zona_oeste_centro" // Grupo A (Astúrias/Centro)
    const val ZONE_EAST_PEREQUE = "zona_leste_pereque" // Grupo B (Perequê/Enseada Fundão)

    // Bairro -> Loja Responsável
    val neighborhoodMap = mapOf(
        // ZONA OESTE / CENTRO (Grupo A)
        "Astúrias" to STORE_ID_ASTURIAS,
        "Pitangueiras" to STORE_ID_ASTURIAS,
        "Tombo" to STORE_ID_ASTURIAS,
        "Guaiúba" to STORE_ID_ASTURIAS,
        "Jardim Las Palmas" to STORE_ID_ASTURIAS,
        "Vila Moisés" to STORE_ID_ASTURIAS,

        "Santa Rosa" to STORE_ID_ASTURIAS, // Temp: Redirecionando para Astúrias
        "Santo Antônio" to STORE_ID_ASTURIAS,
        "Vila Lígia" to STORE_ID_ASTURIAS,
        "Jardim dos Pássaros" to STORE_ID_ASTURIAS,
        "Vicente de Carvalho" to STORE_ID_ASTURIAS,
        "Paecará" to STORE_ID_ASTURIAS,
        "Itapema" to STORE_ID_ASTURIAS,

        "Morrinhos" to STORE_ID_ASTURIAS, // Temp: Redirecionando para Astúrias
        "Vila Zilda" to STORE_ID_ASTURIAS,
        "Cachoeira" to STORE_ID_ASTURIAS,
        "Vila Edna" to STORE_ID_ASTURIAS,

        // ZONA LESTE (Grupo B)
        "Perequê" to STORE_ID_ASTURIAS, // Temp: Redirecionando tudo para central
        "Jardim Acapulco" to STORE_ID_ASTURIAS,
        "Praia de Pernambuco" to STORE_ID_ASTURIAS,
        "Mar Casado" to STORE_ID_ASTURIAS,

        "Enseada" to STORE_ID_ASTURIAS,
        "Cidade Atlântica" to STORE_ID_ASTURIAS,
        "Jardim Virgínia" to STORE_ID_ASTURIAS,
        "Pedreira" to STORE_ID_ASTURIAS,
        "Vila Rã" to STORE_ID_ASTURIAS
    )

    // --- FUNÇÕES UTILITÁRIAS ---

    fun getNeighborhoods(): List<String> = neighborhoodMap.keys.sorted()

    fun getPreferredStore(neighborhood: String): String {
        return neighborhoodMap[neighborhood] ?: STORE_ID_ASTURIAS
    }

    // Qual zona esta loja pertence?
    fun getDeliveryZoneForStore(storeId: String): String {
        return when(storeId) {
            STORE_ID_ASTURIAS, STORE_ID_MORRINHOS, STORE_ID_STAROSA -> ZONE_WEST_CENTER
            STORE_ID_PEREQUE1, STORE_ID_PEREQUE2 -> ZONE_EAST_PEREQUE
            else -> ZONE_WEST_CENTER
        }
    }


    
    // Zonas distantes fecham mais cedo (ex: 30-60 min antes da loja principal)
    fun getClosingOffsetMinutes(neighborhood: String): Int {
        return when (neighborhood) {
            "Perequê", "Jardim Acapulco", "Praia de Pernambuco", "Mar Casado" -> 60 // Fecha 1h antes
            "Morrinhos", "Vila Zilda" -> 30 // Fecha 30min antes
            else -> 0
        }
    }

    fun getStoresByZone(zone: String): List<String> {
        // Normalização para evitar erros de digitação/caixa
        val normalizedZone = zone.trim().lowercase()

        return when {
            normalizedZone == ZONE_WEST_CENTER || normalizedZone.contains("oeste") || normalizedZone.contains("centro") || normalizedZone.contains("asturias") || normalizedZone.contains("astúrias") -> {
                listOf(STORE_ID_ASTURIAS, STORE_ID_MORRINHOS, STORE_ID_STAROSA)
            }
            normalizedZone == ZONE_EAST_PEREQUE || normalizedZone.contains("leste") || normalizedZone.contains("pereque") || normalizedZone.contains("perequê") -> {
                listOf(STORE_ID_PEREQUE1, STORE_ID_PEREQUE2)
            }
            else -> emptyList()
        }
    }
}