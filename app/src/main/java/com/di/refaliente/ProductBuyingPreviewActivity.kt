package com.di.refaliente

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.di.refaliente.databinding.ActivityProductBuyingPreviewBinding
import com.di.refaliente.shared.SimpleAddress

class ProductBuyingPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductBuyingPreviewBinding
    private val simpleAddressesItems = ArrayList<SimpleAddress>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBuyingPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addresses.adapter = SimpleAddressAdapter(simpleAddressesItems, layoutInflater)
        // binding.addresses.selectedItem as SimpleAddress
    }
}