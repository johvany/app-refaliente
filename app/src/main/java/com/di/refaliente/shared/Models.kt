package com.di.refaliente.shared

data class PublicationSmall(
    val idPublication: Int,
    val title: String,
    val priceOld: String?,
    val price: String,
    val img: String?,
    val keyUserOwner: String
)