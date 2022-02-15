package com.di.refaliente

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.di.refaliente.databinding.ActivityProductBuyingPreviewBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.SimpleAddressAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

class ProductBuyingPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductBuyingPreviewBinding
    private val simpleAddressesItems = ArrayList<SimpleAddress>()
    private lateinit var customAlertDialog: CustomAlertDialog
    private val decimalFormat = DecimalFormat("#,###,###,##0.00")
    private val numberFormatHelper = NumberFormatHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBuyingPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ... get user addresses and load them in the spinner ...
        binding.addresses.adapter = SimpleAddressAdapter(simpleAddressesItems, layoutInflater)
        // binding.addresses.selectedItem as SimpleAddress // ... use this to get selected address ...

        customAlertDialog = CustomAlertDialog(this)

        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            getProductDataBeforeBuyIt(intent.extras!!.getString("id_publication")!!)
            getUserAddresses("138")
        }
    }

    private fun getUserAddresses(idUser: String) {
        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "get-addresses-by-user?key_user=" + idUser,
            null,
            { response ->
                // ...
            },
            { error ->
                try {
                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                // TODO: don't forget remove this hardcode
                return mutableMapOf(Pair("Authorization", "user-token-here"))
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun getProductDataBeforeBuyIt(idPublication: String) {
        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "products/data-before-buy-it?id_publication=" + idPublication,
            null,
            { response ->
                loadProductData(response)
            },
            { error ->
                try {
                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                // TODO: don't forget remove this hardcode
                return mutableMapOf(Pair("Authorization", "user-token-here"))
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    @SuppressLint("SetTextI18n")
    private fun loadProductData(data: JSONObject) {
        val publicationData = data.getJSONObject("publication")

        binding.productTitle.text = publicationData.getString("title")
        binding.productAmount.text = "1 x $" + decimalFormat.format(numberFormatHelper.strToDouble(publicationData.getString("product_price")))
        binding.subtotal.text = "$" + decimalFormat.format(numberFormatHelper.strToDouble(data.getString("subtotal")))
        binding.iva.text = "$" + decimalFormat.format(numberFormatHelper.strToDouble(data.getString("iva_amount")))
        binding.discount.text = "$" + decimalFormat.format(numberFormatHelper.strToDouble(data.getString("discount")))
        binding.total.text = "$" + decimalFormat.format(numberFormatHelper.strToDouble(data.getString("total")))

        // Load product image
        getPublicationImg(publicationData.getJSONObject("product").getString("images"))?.let { imgName ->
            Glide.with(this)
                .load(resources.getString(R.string.api_url_storage) + publicationData.getString("key_user") + "/products/" + imgName)
                // .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                // .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(binding.productImg)
        }
    }

    private fun getPublicationImg(imgsStr: String): String? {
        val imgs = try { JSONArray(imgsStr) } catch (err: Exception) { JSONArray("[]") }
        return if (imgs.length() > 0) { imgs.getString(0) } else { null }
    }
}