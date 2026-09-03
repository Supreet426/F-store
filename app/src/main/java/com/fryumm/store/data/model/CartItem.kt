package com.fryumm.store.data.model

data class CartItem(
    val product: Product = Product(),
    val quantity: Int = 1,
    val selectedSize: String = ""
)
