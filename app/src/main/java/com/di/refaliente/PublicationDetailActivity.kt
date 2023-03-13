package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.databinding.ActivityPublicationDetailBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.databinding.RowItemProductCommentBinding
import com.di.refaliente.databinding.RowItemRelatedProductBinding
import com.di.refaliente.home_menu_ui.PublicationsFragment
import com.di.refaliente.shared.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

class PublicationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPublicationDetailBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private val numberFormatHelper = NumberFormatHelper()
    private var idPublication = ""
    private var publicationKeyUser = 0
    private var productInFavorites = false
    private var existence = 0
    private var selectedQuantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding and load it in this activity.
        binding = ActivityPublicationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize or set variables.
        customAlertDialog = CustomAlertDialog(this)
        idPublication = intent.extras!!.getInt("id_publication").toString()
        binding.addToFavorites.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        binding.quantity.text = selectedQuantity.toString()

        // Set click listeners.
        binding.buyProduct.setOnClickListener { buyProduct() }
        binding.addToFavorites.setOnClickListener { addProductToFavorites() }
        binding.addToShoppingCart.setOnClickListener { addProductToShoppingCart() }
        binding.backArrow.setOnClickListener { finish() }
        binding.quantityRemove.setOnClickListener { decrementsQuantity() }
        binding.quantityAdd.setOnClickListener { incrementsQuantity() }

        // Get publication data and load it in the view.
        getPublicationById(idPublication)
    }

    /**
     * This decrements the selected quantity in -1, only if the product existence is not zero and
     * the selected quantity is not 1, then the quantity element in the view is refreshed.
     */
    private fun decrementsQuantity() {
        if (existence != 0) {
            if (selectedQuantity != 1) {
                selectedQuantity -= 1
                binding.quantity.text = selectedQuantity.toString()
            }
        }
    }

    /**
     * This increments the selected quantity in +1, only if the product existence is not zero and
     * the selected quantity is not equal to the product existence, then the quantity element in
     * the view is refreshed.
     */
    private fun incrementsQuantity() {
        if (existence != 0) {
            if (selectedQuantity != existence) {
                selectedQuantity += 1
                binding.quantity.text = selectedQuantity.toString()
            }
        }
    }

    private fun buyProduct() {
        if (SessionHelper.userLogged()) {
            if (publicationKeyUser == SessionHelper.user!!.sub) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Publicación propia")
                    .setMessage("No se puede realizar la compra. Usted no puede comprar sus propios productos.")
                    .setCancelable(true)
                    .setPositiveButton("ACEPTAR", null)
                    .show()
            } else {
                startActivity(Intent(this, ProductBuyingPreviewActivity::class.java)
                    .putExtra("id_publication", idPublication)
                    .putExtra("quantity", selectedQuantity.toString()))
            }
        } else {
            SessionHelper.showRequiredSessionMessage(this)
        }
    }

    private fun addProductToFavorites() {
        if (SessionHelper.userLogged()) {
            if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
                Utilities.showUnconnectedMessage(customAlertDialog)
            } else {
                performAddProductToFavorites(true)
            }
        } else {
            SessionHelper.showRequiredSessionMessage(this)
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun performAddProductToFavorites(canRepeat: Boolean) {
        val requestMethod = Request.Method.POST
        val requestURL = resources.getString(R.string.api_url) + "user/favorites-list/update"
        val requestData = JSONObject()
        requestData.put("id_user", SessionHelper.user!!.sub)
        requestData.put("id_publication", idPublication)

        val onRequestResponse = Response.Listener<JSONObject> { response ->
            productInFavorites = !productInFavorites

            if (productInFavorites) {
                binding.addToFavorites.imageTintList = ColorStateList.valueOf(Color.parseColor("#DA4444"))
            } else {
                binding.addToFavorites.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            }

            setResult(PublicationsFragment.FAVORITES_LIST_CHANGED)
        }

        val onRequestError = Response.ErrorListener { error ->
            SessionHelper.handleRequestError(error, this, customAlertDialog) {
                if (canRepeat) {
                    performAddProductToFavorites(false)
                } else {
                    Utilities.showRequestError(customAlertDialog, null)
                }
            }
        }

        val request = object: JsonObjectRequest(requestMethod, requestURL, requestData, onRequestResponse, onRequestError) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
            }
        }

        request.retryPolicy = DefaultRetryPolicy(ConstantValues.REQUEST_TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Utilities.queue?.add(request)
    }

    private fun addProductToShoppingCart() {
        if (SessionHelper.userLogged()) {
            if (publicationKeyUser == SessionHelper.user!!.sub) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Publicación propia")
                    .setMessage("No se puede realizar la compra. Usted no puede comprar sus propios productos.")
                    .setCancelable(true)
                    .setPositiveButton("ACEPTAR", null)
                    .show()
            } else {
                performAddProductToShoppingCart(true)
            }
        } else {
            SessionHelper.showRequiredSessionMessage(this)
        }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun performAddProductToShoppingCart(canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.PUT,
                resources.getString(R.string.api_url) + "add-publication-to-shopping-cart",
                JSONObject()
                    .put("key_publication", idPublication)
                    .put("key_user", SessionHelper.user!!.sub)
                    .put("quantity", selectedQuantity),
                { response ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Producto agregado")
                        .setMessage("Se agregó el producto a su carrito de compras.")
                        .setCancelable(false)
                        .setNegativeButton("SEGUIR BUSCANDO", null)
                        .setPositiveButton("VER CARRITO") { _, _ ->
                            setResult(PublicationsFragment.LOAD_SHOPPING_CART)
                            finish()
                        }
                        .show()
                },
                { error ->
                    SessionHelper.handleRequestError(error, this, customAlertDialog) {
                        if (canRepeat) {
                            performAddProductToShoppingCart(false)
                        } else {
                            try {
                                val jsonErr = JSONObject(error.networkResponse.data.decodeToString())

                                if (jsonErr.has("error_code") && jsonErr.getInt("error_code") == 2) {
                                    MaterialAlertDialogBuilder(this).create().also { dialog ->
                                        dialog.setCancelable(false)

                                        dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                                            viewBinding.icon.setImageResource(R.drawable.error_dialog)
                                            viewBinding.title.visibility = View.GONE
                                            viewBinding.message.text = "La cantidad seleccionada no es válida"
                                            viewBinding.negativeButton.visibility = View.GONE
                                            viewBinding.positiveButton.text = "Aceptar"
                                            viewBinding.positiveButton.setOnClickListener { dialog.dismiss() }
                                        }.root)
                                    }.show()
                                } else {
                                    Utilities.showRequestError(customAlertDialog, null)
                                }
                            } catch (err: Exception) {
                                Utilities.showRequestError(customAlertDialog, null)
                            }
                        }
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
                }
            }.apply {
                retryPolicy = DefaultRetryPolicy(
                    ConstantValues.REQUEST_TIMEOUT,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            })
        }
    }

    private fun getPublicationById(idPublication: String) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "find-publication-by-id?id=" + idPublication,
                null,
                { response ->
                    if (SessionHelper.userLogged()) { checkFavoritesList(true) }
                    fillViewsWithData(response)
                },
                {
                    Utilities.showRequestError(customAlertDialog, "no se pudieron obtener los datos para este producto")
                }
            ) {
                // Set request headers here if you need.
            }.apply {
                retryPolicy = DefaultRetryPolicy(
                    ConstantValues.REQUEST_TIMEOUT,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            })
        }
    }
    private fun checkFavoritesList(canRepeat: Boolean) {
        val requestMethod = Request.Method.GET
        val requestURL = resources.getString(R.string.api_url) + "user/favorites-list?id_user=${SessionHelper.user!!.sub}"
        val requestData = null

        val onRequestResponse = Response.Listener<JSONObject> { response ->
            val favoritesIds = response.getJSONArray("favorites_list")
            var itemId: String
            val limit = favoritesIds.length()

            for (i in 0 until limit) {
                itemId = favoritesIds.getString(i)

                if (itemId == idPublication) {
                    productInFavorites = true
                    binding.addToFavorites.imageTintList = ColorStateList.valueOf(Color.parseColor("#DA4444"))
                    break
                }
            }
        }

        val onRequestError = Response.ErrorListener { error ->
            SessionHelper.handleRequestError(error, this, customAlertDialog) {
                if (canRepeat) {
                    checkFavoritesList(false)
                } else {
                    Utilities.showRequestError(customAlertDialog, null)
                }
            }
        }

        val request = object: JsonObjectRequest(requestMethod, requestURL, requestData, onRequestResponse, onRequestError) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
            }
        }

        request.retryPolicy = DefaultRetryPolicy(ConstantValues.REQUEST_TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Utilities.queue?.add(request)
    }

    @SuppressLint("SetTextI18n")
    private fun fillViewsWithData(response: JSONObject) {
        val publicationData = response.getJSONObject("publication")
        val productData = publicationData.getJSONObject("product")
        // val sellerData = publicationData.getJSONObject("user")

        publicationKeyUser = publicationData.getInt("key_user")
        binding.publicationTitle.text = publicationData.getString("title")
        handleProductCondition(productData.getInt("key_condition"))
        handlePublicationStars(productData.getInt("qualification"))
        handleProductDiscount(publicationData.getInt("has_discount"))
        binding.productSold.text = productData.getString("sales_accountant") + " vendidos"
        binding.publicationDescription.text = publicationData.getString("description")
        binding.productExistence.text = productData.getString("existence")
        existence = productData.getInt("existence")

        if (productData.getInt("existence") <= 0) {
            binding.productSoldOutMsg.visibility = View.VISIBLE
        }

        if (publicationData.getInt("has_discount") == 1) {
            binding.productPriceOld.visibility = View.VISIBLE
            binding.productPriceOld.text = "MXN $" + numberFormatHelper.format2Decimals(publicationData.getString("previous_price"))
            binding.productPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.productPriceOld.visibility = View.INVISIBLE
        }

        binding.productPrice.text = "MXN $" + numberFormatHelper.format2Decimals(publicationData.getString("product_price"))
        //binding.sellerName.text = sellerData.getString("name")

        // Load product image.
        getPublicationImg(productData.getString("images"))?.let { imgStr ->
            Glide.with(this)
                .load(resources.getString(R.string.api_url_storage) + productData.getString("key_user") + "/products/" + imgStr)
                .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(binding.publicationImg)
        }

        getProductComments(publicationData.getString("id_publication"))
        getRelatedProducts(productData.getString("id_product"))
    }

    private fun getProductComments(idPublication: String) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "publication/comments?page=1&id_publication=" + idPublication,
            null,
            { response ->
                addProductComments(response)
            },
            {
                Utilities.showRequestError(customAlertDialog, "No se pudieron obtener los comentarios para este producto.")
            }
        ) {
            // Set request headers here if you need.
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER", "SetTextI18n")
    private fun getRelatedProducts(idProduct: String) {
        Utilities.queue?.add(
        object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "publications/related?id_product=" + idProduct,
            null,
            { response ->
                val data = response.getJSONArray("data")
                val dataSize = data.length()
                var item: JSONObject

                for (i in 0 until dataSize step 1) {
                    item = data.getJSONObject(i)
                    val idPublication = item.getInt("id_publication")

                    RowItemRelatedProductBinding.inflate(layoutInflater, binding.relatedProductsContainer, true).also { viewBinding ->
                        // -----------------------------
                        // Set image
                        // -----------------------------

                        getPublicationImg(item.getString("images"))?.let { imgStr ->
                            Glide.with(this)
                                .load(resources.getString(R.string.api_url_storage) + item.getString("key_user") + "/products/" + imgStr)
                                .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                                .into(viewBinding.publicationImg)
                        }

                        // -----------------------------
                        // Set title
                        // -----------------------------

                        viewBinding.publicationTitle.text = item.getString("title")

                        if (viewBinding.publicationTitle.text.length > 75) {
                            viewBinding.publicationTitle.text = viewBinding.publicationTitle.text.substring(0, 75) + "..."
                        }

                        // -----------------------------
                        // Set price (previous)
                        // -----------------------------

                        if (item.getInt("has_discount") == 1) {
                            viewBinding.publicationPriceOld.visibility = View.VISIBLE
                            viewBinding.publicationPriceOld.text = "MXN $" + numberFormatHelper.format2Decimals(item.getString("previous_price"))
                            viewBinding.publicationPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            viewBinding.publicationPriceOld.visibility = View.INVISIBLE
                        }

                        // -----------------------------
                        // Set price (current)
                        // -----------------------------

                        viewBinding.publicationPrice.text = "MXN $" + numberFormatHelper.format2Decimals(item.getString("product_price"))
                    }.root.setOnClickListener {
                        setResult(PublicationsFragment.LOAD_SPECIFIED_PUBLICATION, Intent().putExtra("id_publication", idPublication))
                        finish()

                        /* startActivity(Intent(this, PublicationDetailActivity::class.java)
                            .putExtra("id_publication", idPublication)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)) */
                    }
                }
            },
            { error ->
                // ...
            }
        ) {
            // Set request headers here if you need.
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    @SuppressLint("SetTextI18n")
    private fun addProductComments(response: JSONObject) {
        val comments = response.getJSONArray("comments")
        val limit = comments.length()
        var item: JSONObject

        if (limit > 0) {
            binding.commentsTitle.visibility = View.VISIBLE
        } else {
            binding.commentsTitle.visibility = View.GONE
        }

        for (i in 0 until limit) {
            item = comments.getJSONObject(i)

            RowItemProductCommentBinding.inflate(layoutInflater, binding.commentsContainer, false).let { commentItemBinding ->
                commentItemBinding.productComment.text = item.getString("comment")
                commentItemBinding.customerName.text = item.getString("customer_name") + " - " + item.getString("created_at")
                handleCommentStars(commentItemBinding, item.getInt("qualification"))

                Glide.with(this)
                    .load(resources.getString(R.string.api_url_storage) + item.getString("customer_id") + "/profile/" + item.getString("customer_image"))
                    .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                    .into(commentItemBinding.customerImg)

                binding.commentsContainer.addView(commentItemBinding.root)
            }
        }
    }

    private fun handleCommentStars(viewBinding: RowItemProductCommentBinding, stars: Int) {
        when (stars) {
            1 -> {
                viewBinding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            2 -> {
                viewBinding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            3 -> {
                viewBinding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            4 -> {
                viewBinding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            5 -> {
                viewBinding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                viewBinding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
            }
            else -> {
                viewBinding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                viewBinding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
        }
    }

    private fun handleProductDiscount(hasDiscount: Int) {
        if (hasDiscount == 1) {
            binding.ribbonDiscount.visibility = View.VISIBLE
            binding.ribbonDiscountText.visibility = View.VISIBLE
        } else {
            binding.ribbonDiscount.visibility = View.INVISIBLE
            binding.ribbonDiscountText.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleProductCondition(idCondition: Int) {
        when (idCondition) {
            1 -> {
                binding.ribbon.setImageResource(R.drawable.ribbon_new)
                binding.ribbonText.text = "Nuevo"
            }
            2 -> {
                binding.ribbon.setImageResource(R.drawable.ribbon_used)
                binding.ribbonText.text = "Usado"
            }
            3 -> {
                binding.ribbon.setImageResource(R.drawable.ribbon_refurbished)
                binding.ribbonText.text = "Seminuevo"
            }
        }
    }

    private fun handlePublicationStars(stars: Int) {
        when (stars) {
            1 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            2 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            3 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            4 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            5 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
            }
            else -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
        }
    }

    private fun getPublicationImg(imgsStr: String): String? {
        val imgs = try { JSONArray(imgsStr) } catch (err: Exception) { JSONArray("[]") }
        return if (imgs.length() > 0) { imgs.getString(0) } else { null }
    }
}