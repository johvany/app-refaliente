package com.di.refaliente.home_menu_ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.HomeMenuActivity
import com.di.refaliente.PublicationDetailActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.FragmentFavoritesBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.FavoritesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

class FavoritesFragment : Fragment() {
    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private lateinit var favoritesItems: ArrayList<Publication>
    private var idUser = 0
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.favoritesContainer.layoutManager = LinearLayoutManager(requireContext())
        favoritesItems = ArrayList()

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == PublicationsFragment.LOAD_SHOPPING_CART) {
                (requireActivity() as HomeMenuActivity).loadShoppingCart()
            }
        }

        val onProductImgClick = { itemPosition: Int ->
            val intent = Intent(requireContext(), PublicationDetailActivity::class.java)
            intent.putExtra("id_publication", favoritesItems[itemPosition].idPublication)
            launcher.launch(intent)
        }

        val onRemoveFromFavoritesClick = { itemPosition: Int ->
            val onClickListener = DialogInterface.OnClickListener { dialogInterface, clickedButton ->
                when (clickedButton) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        removeProductFromFavorites(itemPosition, true)
                    }
                }
            }

            val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            alertDialogBuilder.setCancelable(true)
            alertDialogBuilder.setTitle("Quitar producto")
            alertDialogBuilder.setMessage("¿Quieres eliminar este producto de tu lista de favoritos?")
            alertDialogBuilder.setNegativeButton("NO", null)
            alertDialogBuilder.setPositiveButton("SI", onClickListener)
            alertDialogBuilder.create().show()
        }

        binding.favoritesContainer.adapter = FavoritesAdapter(favoritesItems, requireContext(), onProductImgClick, onRemoveFromFavoritesClick)
        customAlertDialog = CustomAlertDialog(requireContext())
        idUser = SessionHelper.user!!.sub
        binding.refresh.setOnRefreshListener { refresh() }
        refresh()
    }

    private fun refresh() {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            getFavoritesList(true)
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun removeProductFromFavorites(itemPosition: Int, canRepeat: Boolean) {
        val requestMethod = Request.Method.POST
        val requestURL = resources.getString(R.string.api_url) + "user/favorites-list/update"
        val requestData = JSONObject()
        requestData.put("id_user", SessionHelper.user!!.sub)
        requestData.put("id_publication", favoritesItems[itemPosition].idPublication)

        val onRequestResponse = Response.Listener<JSONObject> { response ->
            favoritesItems.removeAt(itemPosition)
            binding.favoritesContainer.adapter?.notifyItemRemoved(itemPosition)
            binding.favoritesContainer.adapter?.notifyItemRangeChanged(itemPosition, favoritesItems.size)
        }

        val onRequestError = Response.ErrorListener { error ->
            SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                if (canRepeat) {
                    removeProductFromFavorites(itemPosition, false)
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

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun getFavoritesList(canRepeat: Boolean) {
        val requestMethod = Request.Method.GET
        val requestURL = resources.getString(R.string.api_url) + "user/favorites-list?id_user=$idUser"
        val requestData = null

        val onRequestResponse = Response.Listener<JSONObject> { response ->
            getFavoritesListData(response.getString("favorites_list"))
        }

        val onRequestError = Response.ErrorListener { error ->
            SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                if (canRepeat) {
                    getFavoritesList(false)
                } else {
                    binding.refresh.isRefreshing = false
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

    private fun getFavoritesListData(favoritesListIdsStr: String) {
        val requestMethod = Request.Method.GET
        val requestURL = resources.getString(R.string.api_url) + "publications/find-by-ids?ids=$favoritesListIdsStr"
        val requestData = null

        val onRequestResponse = Response.Listener<JSONObject> { response ->
            binding.refresh.isRefreshing = false
            fillFavoritesItems(response.getJSONArray("publications"))
        }

        val onRequestError = Response.ErrorListener { error ->
            binding.refresh.isRefreshing = false

            try {
                Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
            } catch (err: Exception) {
                Utilities.showRequestError(customAlertDialog, error.toString())
            }
        }

        val request = object: JsonObjectRequest(requestMethod, requestURL, requestData, onRequestResponse, onRequestError) { /* ... */ }
        request.retryPolicy = DefaultRetryPolicy(ConstantValues.REQUEST_TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Utilities.queue?.add(request)
    }

    private fun fillFavoritesItems(publications: JSONArray) {
        val itemsCount = favoritesItems.size
        favoritesItems.clear()
        binding.favoritesContainer.adapter?.notifyItemRangeRemoved(0, itemsCount)

        var publicationItem: JSONObject
        var productItem: JSONObject
        val limit = publications.length()

        // Publication data
        var publicationIdPublication: String
        var publicationTitle: String
        var publicationDescription: String
        var publicationProductPrice: String
        var publicationKeyUser: String
        var publicationKeyUserAddress: String
        var publicationKeyProduct: String
        var publicationKeyPublicationStatus: String
        var publicationKeyPackingAddress: String
        var publicationHasDiscount: String
        var publicationDiscountPercent: String
        var publicationPreviousPrice: String
        var publicationProduct: Product

        // Publication product data
        var productIdProduct: String
        var productCodeTM: String
        var productName: String
        var productDescription: String
        var productPrice: String
        var productExistence: String
        var productQuantity: String
        var productKeyUser: String
        var productKeyCondition: String
        var productKeyProductStatus: String
        var productKeyMeasurementUnits: String
        var productImages: String
        var productSalesAccountant: String
        var productQualification: String
        var productQualificationAvg: String
        var productHasDiscount: String
        var productDiscountPercent: String
        var productPreviousPrice: String
        var productCreatedAt: String
        var productUpdatedAt: String

        for (i in 0 until limit) {
            publicationItem = publications.getJSONObject(i)
            productItem = publicationItem.getJSONObject("product")

            // Publication data
            publicationIdPublication = publicationItem.getString("id_publication")
            publicationTitle = publicationItem.getString("title")
            publicationDescription = publicationItem.getString("description")
            publicationProductPrice = publicationItem.getString("product_price")
            publicationKeyUser = publicationItem.getString("key_user")
            publicationKeyUserAddress = publicationItem.getString("key_user_address")
            publicationKeyProduct = publicationItem.getString("key_product")
            publicationKeyPublicationStatus = publicationItem.getString("key_publication_status")
            publicationKeyPackingAddress = publicationItem.getString("key_packing_address")
            publicationHasDiscount = publicationItem.getString("has_discount")
            publicationDiscountPercent = publicationItem.getString("discount_percent")
            publicationPreviousPrice = publicationItem.getString("previous_price")

            // Publication product data
            productIdProduct = productItem.getString("id_product")
            productCodeTM = productItem.getString("code_tm")
            productName = productItem.getString("name")
            productDescription = productItem.getString("description")
            productPrice = productItem.getString("price")
            productExistence = productItem.getString("existence")
            productQuantity = productItem.getString("quantity")
            productKeyUser = productItem.getString("key_user")
            productKeyCondition = productItem.getString("key_condition")
            productKeyProductStatus = productItem.getString("key_product_status")
            productKeyMeasurementUnits = productItem.getString("key_measurement_units")
            productImages = productItem.getString("images")
            productSalesAccountant = productItem.getString("sales_accountant")
            productQualification = productItem.getString("qualification")
            productQualificationAvg = productItem.getString("qualification_avg")
            productHasDiscount = productItem.getString("has_discount")
            productDiscountPercent = productItem.getString("discount_percent")
            productPreviousPrice = productItem.getString("previous_price")
            productCreatedAt = productItem.getString("created_at")
            productUpdatedAt = productItem.getString("updated_at")

            publicationProduct = Product(
                productIdProduct.toInt(),
                productCodeTM,
                productName,
                if (productDescription == "null") { null } else { productDescription },
                productPrice,
                productExistence.toInt(),
                productQuantity.toInt(),
                productKeyUser.toInt(),
                productKeyCondition.toInt(),
                productKeyProductStatus.toInt(),
                productKeyMeasurementUnits.toInt(),
                if (productImages == "null") { null } else { productImages },
                productSalesAccountant.toInt(),
                productQualification.toInt(),
                productQualificationAvg,
                if (productHasDiscount == "null") { null } else { productHasDiscount.toInt() },
                if (productDiscountPercent == "null") { null } else { productDiscountPercent.toInt() },
                if (productPreviousPrice == "null") { null } else { productPreviousPrice },
                if (productCreatedAt == "null") { null } else { productCreatedAt },
                if (productUpdatedAt == "null") { null } else { productUpdatedAt },
            )

            favoritesItems.add(Publication(
                publicationIdPublication.toInt(),
                publicationTitle,
                if (publicationDescription == "null") { null } else { publicationDescription },
                publicationProductPrice,
                publicationKeyUser.toInt(),
                publicationKeyUserAddress.toInt(),
                publicationKeyProduct.toInt(),
                publicationKeyPublicationStatus.toInt(),
                if (publicationKeyPackingAddress == "null") { null } else { publicationKeyPackingAddress.toInt() },
                if (publicationHasDiscount == "null") { null } else { publicationHasDiscount.toInt() },
                if (publicationDiscountPercent == "null") { null } else { publicationDiscountPercent.toInt() },
                if (publicationPreviousPrice == "null") { null } else { publicationPreviousPrice },
                publicationProduct,
            ))
        }

        binding.favoritesContainer.adapter?.notifyItemRangeInserted(0, limit)
    }
}