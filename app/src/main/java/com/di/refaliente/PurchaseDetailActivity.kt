package com.di.refaliente

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.di.refaliente.databinding.ActivityPurchaseDetailBinding
import com.di.refaliente.shared.PurchaseDetail
import com.di.refaliente.view_adapters.PurchasesDetailsAdapter
import org.json.JSONArray
import org.json.JSONObject

class PurchaseDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPurchaseDetailBinding
    private val purchaseDetail = ArrayList<PurchaseDetail>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener { finish() }
        binding.comeback.setOnClickListener { finish() }
        binding.purchasesDetails.layoutManager = LinearLayoutManager(this)
        binding.purchasesDetails.adapter = PurchasesDetailsAdapter(purchaseDetail, this)
        fillPurchaseDetail(intent.getStringExtra("purchase_detail")!!)
        binding.title.text = "Compra ID: ${intent.getStringExtra("id_purchase_formatted")}"
    }

    private fun fillPurchaseDetail(purchaseDetailJsonArrayStr: String) {
        val jsonArray = JSONArray(purchaseDetailJsonArrayStr)
        val limit = jsonArray.length()
        var item: JSONObject

        for (i in 0 until limit) {
            item = jsonArray.getJSONObject(i)

            purchaseDetail.add(PurchaseDetail(
                item.getInt("id_sale_detail"),
                item.getString("product_name"),
                if (item.isNull("images")) { null } else { item.getString("images") },
                item.getString("seller_name"),
                item.getInt("key_seller"),
                item.getString("product_price"),
                item.getString("quantity"),
                item.getString("subtotal"),
                item.getString("iva"),
                item.getString("discount"),
                item.getString("total"),
                item.getBoolean("ready_to_comment")
            ))
        }

        binding.purchasesDetails.adapter?.notifyItemRangeInserted(0, limit)
    }
}