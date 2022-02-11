package com.di.refaliente

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.di.refaliente.databinding.ActivityPublicationDetailBinding
import com.di.refaliente.shared.ConnectionHelper
import com.di.refaliente.shared.CustomAlertDialog
import com.di.refaliente.shared.NumberFormatHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

class PublicationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPublicationDetailBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private val decimalFormat = DecimalFormat("#,###,###,##0.00")
    private val numberFormatHelper = NumberFormatHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customAlertDialog = CustomAlertDialog(this)
        getPublicationById(intent.extras!!.getInt("id_publication").toString())
    }

    private fun getPublicationById(idPublication: String) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            customAlertDialog.setTitle("Sin conexión")
            customAlertDialog.setMessage("Por favor revisa tu conexión de internet e intenta de nuevo.")
            customAlertDialog.show()
        } else {
            Volley.newRequestQueue(this).add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "find-publication-by-id?id=" + idPublication,
                null,
                { response ->
                    fillViewsWithData(response)
                },
                { error ->
                    customAlertDialog.setTitle("Obtención de publicaciones fallida")
                    customAlertDialog.setMessage("No se pudieron obtener las publicaciones. Por favor intenta de nuevo y si el problema continua contacta a soporte.")

                    try {
                        customAlertDialog.setErrorDetail(error.networkResponse.data.decodeToString())
                    } catch (err: Exception) {
                        customAlertDialog.setErrorDetail(error.toString())
                    }

                    customAlertDialog.show()
                }
            ) {
                // Set request headers here if you need.
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
        binding.productSold.text = productData.getString("sales_accountant") + " vendidos"

        if (publicationData.getInt("has_discount") == 1) {
            binding.productPriceOld.visibility = View.VISIBLE
            binding.productPriceOld.text = "MXN $" + decimalFormat.format(numberFormatHelper.strToDouble(publicationData.getString("previous_price")))
            binding.productPriceOld.paintFlags = binding.productPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.productPriceOld.visibility = View.INVISIBLE
        }

        binding.productPrice.text = "MXN $" + decimalFormat.format(numberFormatHelper.strToDouble(publicationData.getString("product_price")))
        binding.sellerName.text = sellerData.getString("name")

        getPublicationImg(productData.getString("images"))?.let { imgStr ->
            Glide.with(this)
                .load(resources.getString(R.string.api_url_storage) + productData.getString("key_user") + "/products/" + imgStr)
                // .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                // .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(binding.publicationImg)
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

    private fun handlePublicationStars(starts: Int) {
        when (starts) {
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