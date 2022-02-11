package com.di.refaliente.ui.home_menu.publications

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.di.refaliente.PublicationDetailActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.FragmentPublicationsBinding
import com.di.refaliente.shared.ConnectionHelper
import com.di.refaliente.shared.CustomAlertDialog
import com.di.refaliente.shared.PublicationSmall
import org.json.JSONArray
import org.json.JSONObject

class PublicationsFragment : Fragment() {
    private lateinit var binding: FragmentPublicationsBinding
    private val publicationsItems = ArrayList<PublicationSmall>()
    private lateinit var customAlertDialog: CustomAlertDialog
    private var currentPage = 1
    private var lastPage = 1
    private var gettingPublications = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPublicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.publications.layoutManager = LinearLayoutManager(requireContext())
        binding.publications.adapter = PublicationsAdapter(publicationsItems, requireContext()) { idPublication -> showPublication(idPublication) }
        binding.publications.recycledViewPool.setMaxRecycledViews(1, 50)
        customAlertDialog = CustomAlertDialog(requireContext())

        binding.refresh.setOnRefreshListener {
            currentPage = 1
            lastPage = 1
            publicationsItems.clear()
            getPublications(currentPage.toString())
        }

        binding.publications.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!binding.publications.canScrollVertically(1) && !gettingPublications) {
                    if (currentPage < lastPage) {
                        currentPage++
                        getPublications(currentPage.toString())
                    }
                }
            }
        })

        getPublications(currentPage.toString())
    }

    private fun showPublication(idPublication: Int) {
        startActivity(Intent(requireContext(), PublicationDetailActivity::class.java).putExtra("id_publication", idPublication))
    }

    private fun getPublications(currentPage: String) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            customAlertDialog.setTitle("Sin conexión")
            customAlertDialog.setMessage("Por favor revisa tu conexión de internet e intenta de nuevo.")
            customAlertDialog.show()
        } else {
            gettingPublications = true

            Volley.newRequestQueue(requireContext()).add(object: JsonObjectRequest(
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

    @SuppressLint("NotifyDataSetChanged")
    private fun loadPublications(items: JSONArray) {
        val limit = items.length()
        var jsonItem: JSONObject
        val positionToScroll = publicationsItems.size

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

        binding.publications.adapter?.notifyDataSetChanged()
        if (positionToScroll < publicationsItems.size) { binding.publications.scrollToPosition(positionToScroll) }
    }

    private fun getPublicationImg(imgsStr: String): String? {
        val imgs = try { JSONArray(imgsStr) } catch (err: Exception) { JSONArray("[]") }
        return if (imgs.length() > 0) { imgs.getString(0) } else { null }
    }
}