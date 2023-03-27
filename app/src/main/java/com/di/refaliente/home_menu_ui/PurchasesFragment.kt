package com.di.refaliente.home_menu_ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.PurchaseDetailActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.FragmentPurchasesBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.PurchasesHeadersAdapter
import org.json.JSONArray
import org.json.JSONObject

class PurchasesFragment : Fragment() {
    companion object {
        const val NEW_PRODUCT_COMMENT = 1
    }

    private lateinit var binding: FragmentPurchasesBinding
    private val purchasesHeadersItems = ArrayList<PurchaseHeader>()
    private var currentPage = 1
    private var lastPage = 1
    private var gettingPurchases = false
    private var itemsRemoved = 0
    private var scrollLimitReached = false
    private lateinit var customAlertDialog: CustomAlertDialog
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPurchasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVars()

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == NEW_PRODUCT_COMMENT) {
                refreshPurchases()
            }
        }

        binding.purchases.layoutManager = LinearLayoutManager(requireContext())

        binding.purchases.adapter = PurchasesHeadersAdapter(purchasesHeadersItems, requireContext()) { itemPosition ->
            launcher.launch(Intent(requireContext(), PurchaseDetailActivity::class.java)
                .putExtra("purchase_detail", purchasesHeadersItems[itemPosition].productsDetail)
                .putExtra("id_purchase_formatted", purchasesHeadersItems[itemPosition].idPurchaseFormatted))
        }

        customAlertDialog = CustomAlertDialog(requireContext())
        binding.refresh.setOnRefreshListener { refreshPurchases() }

        binding.purchases.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                handlePublicationsScrollEvent()
            }
        })

        getPurchases(currentPage.toString(), true)
    }

    private fun handlePublicationsScrollEvent() {
        if (!binding.purchases.canScrollVertically(1) && !gettingPurchases) {
            if (currentPage < lastPage) {
                currentPage++
                scrollLimitReached = true
                getPurchases(currentPage.toString(), true)
            }
        }
    }

    private fun getPurchases(currentPage: String, canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            binding.refresh.isRefreshing = false
            gettingPurchases = false
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            binding.refresh.isRefreshing = true
            gettingPurchases = true

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "get-purchases-by-user?page=" + currentPage + "&key_user=" + SessionHelper.user!!.sub.toString(),
                null,
                { response ->
                    binding.refresh.isRefreshing = false
                    gettingPurchases = false
                    val purchases = response.getJSONObject("purchases")
                    lastPage = purchases.getInt("last_page")
                    loadPurchases(purchases.getJSONArray("data"))
                },
                { error ->
                    binding.refresh.isRefreshing = false
                    gettingPurchases = false

                    SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                        if (canRepeat) {
                            getPurchases(currentPage, false)
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
            })
        }
    }

    private fun loadPurchases(items: JSONArray) {
        val limit = items.length()
        var jsonItem: JSONObject
        val oldSize = purchasesHeadersItems.size

        for (i in 0 until limit) {
            jsonItem = items.getJSONObject(i)

            purchasesHeadersItems.add(PurchaseHeader(
                jsonItem.getInt("id_purchase"),
                jsonItem.getString("id_purchase_formatted"),
                jsonItem.getString("created_at_short"),
                jsonItem.getString("customer_name"),
                if (jsonItem.getString("customer_phone") == "null") { null } else { jsonItem.getString("customer_phone") },
                jsonItem.getString("customer_email"),
                jsonItem.getString("customer_address"),
                jsonItem.getString("customer_zipcode"),
                jsonItem.getString("products_name_full"),
                jsonItem.getString("products_detail")
            ))
        }

        if (purchasesHeadersItems.size > oldSize) {
            binding.purchases.adapter?.notifyItemRangeInserted(oldSize, limit)
            if (scrollLimitReached) { binding.purchases.scrollToPosition(oldSize) }
        }

        scrollLimitReached = false

        if (purchasesHeadersItems.size > 0) {
            binding.messageTitle.visibility = View.INVISIBLE
            binding.message.visibility = View.INVISIBLE
        } else {
            binding.messageTitle.visibility = View.VISIBLE
            binding.message.visibility = View.VISIBLE
        }
    }

    private fun refreshPurchases() {
        currentPage = 1
        lastPage = 1
        itemsRemoved = purchasesHeadersItems.size
        purchasesHeadersItems.clear()
        binding.purchases.adapter?.notifyItemRangeRemoved(0, itemsRemoved)
        getPurchases(currentPage.toString(), true)
    }

    private fun initVars() {
        purchasesHeadersItems.clear()
        currentPage = 1
        lastPage = 1
        gettingPurchases = false
        itemsRemoved = 0
        scrollLimitReached = false
    }
}