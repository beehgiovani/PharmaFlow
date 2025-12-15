// Em data/model/User.kt (Atualize ou substitua)
package com.developersbeeh.pharmaflow.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val whatsapp: String = "",
    val neighborhood: String = "", // Bairro do Guarujá
    val address: String = "",      // Rua e Número
    val authorizedContact: Boolean = false,
    val role: String = "client",   // client, admin, driver, manager
    val active: Boolean = true,    // Motoboy nasce false até admin aprovar?
    val driverZone: String = "",   // Zona do motoboy (Group A ou B)
    val assignedStoreId: String = "",
    val points: Int = 0
)