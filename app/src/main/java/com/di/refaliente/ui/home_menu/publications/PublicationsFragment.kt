package com.di.refaliente.ui.home_menu.publications

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPublicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.publications.layoutManager = LinearLayoutManager(requireContext())
        binding.publications.adapter = PublicationsAdapter(publicationsItems, requireContext())
        customAlertDialog = CustomAlertDialog(requireContext())

        binding.refresh.setOnRefreshListener {
            publicationsItems.clear()
            getPublications()
        }

        getPublications()
    }

    private fun getPublications() {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            customAlertDialog.setTitle("Sin conexión")
            customAlertDialog.setMessage("Por favor revisa tu conexión de internet e intenta de nuevo.")
            customAlertDialog.show()
        } else {
            Volley.newRequestQueue(requireContext()).add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "get-all-publications?page=1",
                null,
                { response ->
                    binding.refresh.isRefreshing = false
                    loadPublications(response.getJSONObject("publications").getJSONArray("data"))
                },
                { error ->
                    binding.refresh.isRefreshing = false
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

        for (i in 0 until limit) {
            jsonItem = items.getJSONObject(i)

            publicationsItems.add(PublicationSmall(
                jsonItem.getString("title"),
                if (jsonItem.getInt("has_discount") == 0) { null } else { jsonItem.getString("previous_price") },
                jsonItem.getString("product_price"),
                getPublicationImg(jsonItem.getJSONObject("product").getString("images")),
                jsonItem.getString("key_user")
            ))
        }

        binding.publications.adapter?.notifyDataSetChanged()
    }

    private fun getPublicationImg(imgsStr: String): String? {
        val imgs = try { JSONArray(imgsStr) } catch (err: Exception) { JSONArray("[]") }
        return if (imgs.length() > 0) { imgs.getString(0) } else { null }
    }
}