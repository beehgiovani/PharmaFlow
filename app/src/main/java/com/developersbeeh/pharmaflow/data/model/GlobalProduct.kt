package com.developersbeeh.pharmaflow.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Representa um produto no Catálogo Mestre.
 * Compartilhado entre todas as filiais.
 */
data class GlobalProduct(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ean: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = ""
)