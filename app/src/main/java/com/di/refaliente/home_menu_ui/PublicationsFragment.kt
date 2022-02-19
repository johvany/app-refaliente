package com.di.refaliente.home_menu_ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.PublicationDetailActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.FragmentPublicationsBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.PublicationsAdapter
import org.json.JSONArray
import org.json.JSONObject

class PublicationsFragment : Fragment() {
    private lateinit var binding: FragmentPublicationsBinding
    private val publicationsItems = ArrayList<PublicationSmall>()
    private lateinit var customAlertDialog: CustomAlertDialog
    private var currentPage = 1
    private var lastPage = 1
    private var gettingPublications = false
    private var itemsRemoved = 0
    private var scrollLimitReached = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPublicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initVars()
        binding.publications.layoutManager = LinearLayoutManager(requireContext())
        binding.publications.adapter = PublicationsAdapter(publicationsItems, requireContext()) { idPublication -> showPublication(idPublication) }
        customAlertDialog = CustomAlertDialog(requireContext())
        binding.refresh.setOnRefreshListener { refreshPublications() }

        binding.publications.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                handlePublicationsScrollEvent()
            }
        })

        getPublications(currentPage.toString())
    }

    private fun initVars() {
        publicationsItems.clear()
        currentPage = 1
        lastPage = 1
        gettingPublications = false
        itemsRemoved = 0
        scrollLimitReached = false
    }

    private fun refreshPublications() {
        currentPage = 1
        lastPage = 1
        itemsRemoved = publicationsItems.size
        publicationsItems.clear()
        binding.publications.adapter?.notifyItemRangeRemoved(0, itemsRemoved)
        getPublications(currentPage.toString())
    }

    private fun handlePublicationsScrollEvent() {
        if (!binding.publications.canScrollVertically(1) && !gettingPublications) {
            if (currentPage < lastPage) {
                currentPage++
                scrollLimitReached = true
                getPublications(currentPage.toString())
            }
        }
    }

    private fun showPublication(idPublication: Int) {
        startActivity(Intent(requireContext(), PublicationDetailActivity::class.java).putExtra("id_publication", idPublication))
    }

    private fun getPublications(currentPage: String) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            binding.refresh.isRefreshing = false
            gettingPublications = false
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            binding.refresh.isRefreshing = true
            gettingPublications = true

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "get-all-publications?page=" + currentPage,
                null,
                { response ->
                    binding.refresh.isRefreshing = false
                    gettingPublications = false
                    val publications = response.getJSONObject("publications")
                    lastPage = publications.getInt("last_page")
                    loadPublications(publications.getJSONArray("data"))
                },
                { error ->
                    binding.refresh.isRefreshing = false
                    gettingPublications = false

                    try {
                        Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                    } catch (err: Exception) {
                        Utilities.showRequestError(customAlertDialog, error.toString())
                    }
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

    private fun loadPublications(items: JSONArray) {
        val limit = items.length()
        var jsonItem: JSONObject
        val oldSize = publicationsItems.size

        for (i in 0 until limit) {
            jsonItem = items.getJSONObject(i)

            publicationsItems.add(PublicationSmall(
                jsonItem.getInt("id_publication"),
                jsonItem.getString("title"),
                if (jsonItem.getInt("has_discount") == 0) { null } else { jsonItem.getString("previous_price") },
                jsonItem.getString("product_price"),
                getPublicationImg(jsonItem.getJSONObject("product").getString("images")),
                jsonItem.getString("key_user")
            ))
        }

        if (publicationsItems.size > oldSize) {
            binding.publications.adapter?.notifyItemRangeInserted(oldSize, limit)
            if (scrollLimitReached) { binding.publications.scrollToPosition(oldSize) }
        }

        scrollLimitReached = false
    }

    private fun getPublicationImg(imgsStr: String): String? {
        val imgs = try { JSONArray(imgsStr) } catch (err: Exception) { JSONArray("[]") }
        return if (imgs.length() > 0) { imgs.getString(0) } else { null }
    }
}