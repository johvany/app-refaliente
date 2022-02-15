package com.di.refaliente.home_menu_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.di.refaliente.databinding.FragmentPurchasesBinding

class PurchasesFragment : Fragment() {
    private lateinit var binding: FragmentPurchasesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPurchasesBinding.inflate(inflater, container, false)
        return binding.root
    }
}