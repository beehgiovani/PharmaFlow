package com.developersbeeh.pharmaflow.data.model

import com.google.firebase.firestore.DocumentId

data class Banner(
    @DocumentId
    val id: String = "",
    val imageUrl: String = "",
    val storeId: String = "", // Para mostrar banners específicos da loja (ou geral)
    val active: Boolean = true,
    val order: Int = 0 // Para ordenar qual aparece primeiro
)