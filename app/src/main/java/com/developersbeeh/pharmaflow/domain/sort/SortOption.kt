package com.developersbeeh.pharmaflow.domain.sort

enum class SortOption(val label: String) {
    DEFAULT("Padrão"),
    PRICE_ASC("Menor Preço"),
    PRICE_DESC("Maior Preço"),
    NAME_ASC("Nome (A-Z)")
}
