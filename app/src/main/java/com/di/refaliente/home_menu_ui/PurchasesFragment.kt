package com.di.refaliente.home_menu_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.di.refaliente.databinding.FragmentPurchasesBinding
import com.di.refaliente.shared.PurchaseHeader
import com.di.refaliente.view_adapters.PurchasesHeadersAdapter

class PurchasesFragment : Fragment() {
    private lateinit var binding: FragmentPurchasesBinding
    private val purchasesHeadersItems = ArrayList<PurchaseHeader>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPurchasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ... delete after finish tests ...
        for (i in 1 until 100) { purchasesHeadersItems.add(PurchaseHeader(i.toString())) }

        binding.purchases.layoutManager = LinearLayoutManager(requireContext())
        binding.purchases.adapter = PurchasesHeadersAdapter(purchasesHeadersItems, requireContext())
    }
}