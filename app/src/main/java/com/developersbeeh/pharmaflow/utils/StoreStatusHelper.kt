package com.developersbeeh.pharmaflow.utils

import com.developersbeeh.pharmaflow.data.model.Store
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object StoreStatusHelper {

    fun isStoreOpen(store: Store): Boolean {
        if (!store.active) return false
        if (store.openingTime.isBlank() || store.closingTime.isBlank()) return true // Sem horário definido = sempre aberto

        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("America/Sao_Paulo") // Garante fuso BR

            val now = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo"))
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            val openParts = store.openingTime.split(":").map { it.toInt() }
            val closeParts = store.closingTime.split(":").map { it.toInt() }

            val openTimeInMinutes = openParts[0] * 60 + openParts[1]
            val closeTimeInMinutes = closeParts[0] * 60 + closeParts[1]

            // Suporta virada de dia (ex: abre 18:00 fecha 02:00)
            if (closeTimeInMinutes < openTimeInMinutes) {
                 currentTimeInMinutes >= openTimeInMinutes || currentTimeInMinutes <= closeTimeInMinutes
            } else {
                 currentTimeInMinutes in openTimeInMinutes..closeTimeInMinutes
            }
        } catch (e: Exception) {
            true // Em caso de erro de parse, mantém aberto para não bloquear vendas
        }
    }

    fun isDeliveryTime(store: Store, neighborhood: String = ""): Boolean {
        if (store.deliveryStart.isBlank() || store.deliveryEnd.isBlank()) return true

        return try {
            val now = Calendar.getInstance(TimeZone.getTimeZone("America/Sao_Paulo"))
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            val openParts = store.deliveryStart.split(":").map { it.toInt() }
            val closeParts = store.deliveryEnd.split(":").map { it.toInt() }

            val openTimeInMinutes = openParts[0] * 60 + openParts[1]
            var closeTimeInMinutes = closeParts[0] * 60 + closeParts[1]

            // APLICAR OFFSET DE BAIRRO (Se houver)
            if (neighborhood.isNotEmpty()) {
                val offset = GuarujaUtils.getClosingOffsetMinutes(neighborhood)
                closeTimeInMinutes -= offset
            }

            if (closeTimeInMinutes < openTimeInMinutes) {
                currentTimeInMinutes >= openTimeInMinutes || currentTimeInMinutes <= closeTimeInMinutes
            } else {
                currentTimeInMinutes in openTimeInMinutes..closeTimeInMinutes
            }
        } catch (e: Exception) {
            true
        }
    }
}
