package com.example.hibeauty

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val benefits: String = "",
    val howToUse: String = "",
    val isActive: Boolean = true,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val isOffer: Boolean = false,
    val oldPrice: Long = 0L,
    val presentations: Map<String, ProductPresentation> = emptyMap()
)

data class ProductPresentation(
    val price: Long = 0L,
    val stock: Long = 0L
)
