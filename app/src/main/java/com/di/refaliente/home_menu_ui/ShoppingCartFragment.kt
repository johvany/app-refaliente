package com.di.refaliente.home_menu_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.di.refaliente.databinding.FragmentShoppingCartBinding

class ShoppingCartFragment : Fragment() {
    private lateinit var binding: FragmentShoppingCartBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentShoppingCartBinding.inflate(inflater, container, false)
        return binding.root
    }
}