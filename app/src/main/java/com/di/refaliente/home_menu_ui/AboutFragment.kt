package com.di.refaliente.home_menu_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.R
import com.di.refaliente.databinding.FragmentAboutBinding
import com.di.refaliente.shared.ConnectionHelper
import com.di.refaliente.shared.Utilities

class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ConnectionHelper.getConnectionType(requireContext()) != ConnectionHelper.NONE) {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "messages/about",
                null,
                { response ->
                    binding.text.text = HtmlCompat.fromHtml(response.getString("about_message"), HtmlCompat.FROM_HTML_MODE_LEGACY)
                },
                { error ->
                    // handel error here if you need.
                }
            ) {
                // Set request headers here if you need.
            })
        }
    }
}