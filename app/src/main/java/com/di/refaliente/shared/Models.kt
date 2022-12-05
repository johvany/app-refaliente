package com.di.refaliente.shared

data class Product(
    val idProduct: Int,
    val codeTM: String,
    val name: String,
    val description: String?,
    val price: String,
    val existence: Int,
    val quantity: Int,
    val keyUser: Int,
    val keyCondition: Int,
    val keyProductStatus: Int,
    val keyMeasurementUnits: Int,
    val images: String?,
    val salesAccountant: Int,
    val qualification: Int,
    val qualificationAvg: String,
    val hasDiscount: Int?,
    val discountPercent: Int?,
    val previousPrice: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Publication(
    val idPublication: Int,
    val title: String,
    val description: String?,
    val productPrice: String,
    val keyUser: Int,
    val keyUserAddress: Int,
    val keyProduct: Int,
    val keyPublicationStatus: Int,
    val keyPackingAddress: Int?,
    val hasDiscount: Int?,
    val discountPercent: Int?,
    val previousPrice: String?,
    val product: Product
)

data class UserTypeItem(
    val idUserType: Int,
    val name: String
)

data class PurchaseHeader(
    val idPurchase: Int,
    val idPurchaseFormatted: String,
    val createdAtShort: String,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String,
    val customerAddress: String,
    val customerPostalCode: String,
    val productsNamesFull: String,
    val productsDetail: String
)

data class PurchaseDetail(
    val idSaleDetail: Int,
    val productName: String,
    val images: String?,
    val sellerName: String,
    val keySeller: Int,
    val productPrice: String,
    val quantity: String,
    val subtotal: String,
    val iva: String,
    val discount: String,
    val total: String,
    val readyToComment: Boolean
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

data class BusinessTypeItem(
    val idBusinessType: Int,
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
    val token: String,
    var userDetail: UserDetail?
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

data class Zipcode(
    val idZipcode: Int,
    val zipcode: Int,
    val townshipKey: Int?,
    val townshipName: String,
    val townshipType: String?,
    val townshipTypeCode: Int?,
    val townshipZone: String?,
    val municipalityName: String?,
    val municipalityKey: Int?,
    val entityName: String?,
    val entityKey: Int?,
    val cityName: String?,
    val cityKey: String?,
    val pcAdministration: Int?,
    val pcAdministrationOffice: Int?,
    val createdAt: String?,
    val updatedAt: String?
)

data class SessionAux(
    val idUser: Int,
    val tokenId: Int
)