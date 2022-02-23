package com.di.refaliente.shared

data class PurchaseHeader(
    val idPurchase: Int,
    val idPurchaseFormatted: String,
    val createdAtShort: String,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String,
    val customerAddress: String,
    val customerPostalCode: String
)

data class PublicationSmall(
    val idPublication: Int,
    val title: String,
    val priceOld: String?,
    val price: String,
    val img: String?,
    val keyUserOwner: String
)

data class SimpleAddress(
    val idAddress: Int,
    val name: String
)

data class User(
    val idLocal: Int,
    val sub: Int,
    val email: String,
    val name: String,
    val surname: String,
    val roleUser: String,
    val password: String,
    val token: String
)

data class UserDetail(
    val idLocal: Int,
    val keySub: Int,
    val name: String,
    val surname: String,
    val email: String,
    val description: String?,
    val keyTypeUser: Int,
    val keyBusinessType: Int,
    val telephone: String?,
    val profileImage: String?,
    val sessionType: String,
    val rfc: String?,
    val socialReason: String?,
    val enterpriseName: String?
)

data class CardMonth(
    val value: String
)

data class CardYear(
    val value: String
)