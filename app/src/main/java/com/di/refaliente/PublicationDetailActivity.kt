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
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.databinding.ActivityPublicationDetailBinding
import com.di.refaliente.databinding.RowItemProductCommentBinding
import com.di.refaliente.shared.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

class PublicationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPublicationDetailBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private val decimalFormat = DecimalFormat("#,###,###,##0.00")
    private val numberFormatHelper = NumberFormatHelper()
    private var idPublication = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customAlertDialog = CustomAlertDialog(this)
        idPublication = intent.extras!!.getInt("id_publication").toString()

        binding.buyProduct.setOnClickListener {
            startActivity(Intent(this, ProductBuyingPreviewActivity::class.java).putExtra("id_publication", idPublication))
        }

        getPublicationById(idPublication)
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

    @SuppressLint("SetTextI18n")
    private fun fillViewsWithData(response: JSONObject) {
        val publicationData = response.getJSONObject("publication")
        val productData = publicationData.getJSONObject("product")
        val sellerData = publicationData.getJSONObject("user")

        binding.publicationTitle.text = publicationData.getString("title")
        handleProductCondition(productData.getInt("key_condition"))
        handlePublicationStars(productData.getInt("qualification"))
        handleProductDiscount(publicationData.getInt("has_discount"))
        binding.productSold.text = productData.getString("sales_accountant") + " vendidos"
        binding.publicationDescription.text = publicationData.getString("description")
        binding.productExistence.text = productData.getString("existence")

        if (productData.getInt("existence") <= 0) {
            binding.productSoldOutMsg.visibility = View.VISIBLE
        }

        if (publicationData.getInt("has_discount") == 1) {
            binding.productPriceOld.visibility = View.VISIBLE
            binding.productPriceOld.text = "MXN $" + decimalFormat.format(numberFormatHelper.strToDouble(publicationData.getString("previous_price")))
            binding.productPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.productPriceOld.visibility = View.INVISIBLE
        }

        binding.productPrice.text = "MXN $" + decimalFormat.format(numberFormatHelper.strToDouble(publicationData.getString("product_price")))
        binding.sellerName.text = sellerData.getString("name")

        // Load product image.
        getPublicationImg(productData.getString("images"))?.let { imgStr ->
            Glide.with(this)
                .load(resources.getString(R.string.api_url_storage) + productData.getString("key_user") + "/products/" + imgStr)
                .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(binding.publicationImg)
        }

        getProductComments(publicationData.getString("id_publication"))
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

            RowItemProductCommentBinding.inflate(layoutInflater, binding.publicationContainer, false).let { commentItemBinding ->
                commentItemBinding.productComment.text = item.getString("comment")
                commentItemBinding.customerName.text = item.getString("customer_name") + " - " + item.getString("created_at")
                handleCommentStars(commentItemBinding, item.getInt("qualification"))

                Glide.with(this)
                    .load(resources.getString(R.string.api_url_storage) + item.getString("customer_id") + "/profile/" + item.getString("customer_image"))
                    .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                    .into(commentItemBinding.customerImg)

                binding.publicationContainer.addView(commentItemBinding.root)
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