package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.databinding.ActivityProductBuyingPreviewBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.SimpleAddressAdapter
import org.json.JSONArray
import org.json.JSONObject

class ProductBuyingPreviewActivity : AppCompatActivity() {
    private val requestTag = "ProductBuyingPreviewActivityRequests"

    private lateinit var binding: ActivityProductBuyingPreviewBinding
    private val simpleAddressesItems = ArrayList<SimpleAddress>()
    private lateinit var customAlertDialog: CustomAlertDialog
    private val numberFormatHelper = NumberFormatHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBuyingPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.productTitle.text = ""
        binding.productAmount.text = ""
        customAlertDialog = CustomAlertDialog(this)
        binding.buyProduct.setOnClickListener { buyProduct() }
        getData()
    }

    private fun buyProduct() {
        val selectedAddress = try {
            binding.addresses.selectedItem as SimpleAddress
        } catch (err: Exception) {
            null
        }

        if (selectedAddress == null) {
            // ... Show empty address message here ...
        } else {
            startActivity(Intent(this, PaymentActivity::class.java)
                .putExtra("id_publication", intent.extras!!.getString("id_publication")!!.toInt())
                .putExtra("id_selected_address", selectedAddress.idAddress))
        }
    }

    private fun getData() {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            if (SessionHelper.user != null) {
                getProductDataBeforeBuyIt(intent.extras!!.getString("id_publication")!!, true)
                getUserAddresses(SessionHelper.user!!.sub.toString(), true)
            }
        }
    }

    private fun getProductDataBeforeBuyIt(idPublication: String, canRepeat: Boolean) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "products/data-before-buy-it?id_publication=" + idPublication,
            null,
            { response ->
                loadProductData(response)
            },
            { error ->
                Utilities.queue?.cancelAll(requestTag)

                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        getProductDataBeforeBuyIt(idPublication, false)
                    } else {
                        Utilities.showRequestError(customAlertDialog, null)
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

            tag = requestTag
        })
    }

    @SuppressLint("SetTextI18n")
    private fun loadProductData(data: JSONObject) {
        val publicationData = data.getJSONObject("publication")

        binding.productTitle.text = publicationData.getString("title")
        binding.productAmount.text = "1 x $" + numberFormatHelper.format2Decimals(publicationData.getString("product_price"))
        binding.subtotal.text = "$" + numberFormatHelper.format2Decimals(data.getString("subtotal"))
        binding.iva.text = "$" + numberFormatHelper.format2Decimals(data.getString("iva_amount"))
        binding.discount.text = "$" + numberFormatHelper.format2Decimals(data.getString("discount"))
        binding.total.text = "$" + numberFormatHelper.format2Decimals(data.getString("total"))

        // Load product image
        getPublicationImg(publicationData.getJSONObject("product").getString("images"))?.let { imgName ->
            Glide.with(this)
                .load(resources.getString(R.string.api_url_storage) + publicationData.getString("key_user") + "/products/" + imgName)
                .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(binding.productImg)
        }
    }

    private fun getUserAddresses(idUser: String, canRepeat: Boolean) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "get-addresses-by-user?key_user=" + idUser,
            null,
            { response ->
                loadUserAddresses(response)
            },
            { error ->
                Utilities.queue?.cancelAll(requestTag)

                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        getUserAddresses(idUser, false)
                    } else {
                        Utilities.showRequestError(customAlertDialog, null)
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

            tag = requestTag
        })
    }

    private fun loadUserAddresses(data: JSONObject) {
        data.getJSONArray("addresses").let { addresses ->
            val limit = addresses.length()
            var item: JSONObject
            var itemPostalCode: JSONObject

            for (i in 0 until limit) {
                item = addresses.getJSONObject(i)
                itemPostalCode = item.getJSONObject("zipcode_data")

                simpleAddressesItems.add(SimpleAddress(
                    item.getInt("id_address"),
                    getAddressName(item, itemPostalCode)
                ))
            }

            binding.addresses.adapter = SimpleAddressAdapter(simpleAddressesItems, layoutInflater)
        }
    }

    private fun getAddressName(item: JSONObject, itemPostalCode: JSONObject): String {
        return "${item.getString("street")} " +
                "# ${item.getString("outside_number")}, " +
                "CP ${itemPostalCode.getString("zipcode")}, " +
                "${itemPostalCode.getString("municipality_name")} " +
                itemPostalCode.getString("entity_name")
    }

    private fun getPublicationImg(imgsStr: String): String? {
        val imgs = try { JSONArray(imgsStr) } catch (err: Exception) { JSONArray("[]") }
        return if (imgs.length() > 0) { imgs.getString(0) } else { null }
    }
}