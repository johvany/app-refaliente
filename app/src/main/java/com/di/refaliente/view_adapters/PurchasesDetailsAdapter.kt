package com.di.refaliente.view_adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.di.refaliente.R
import com.di.refaliente.databinding.RowItemPurchaseDetailBinding
import com.di.refaliente.shared.NumberFormatHelper
import com.di.refaliente.shared.PurchaseDetail
import org.json.JSONArray

class PurchasesDetailsAdapter(
    private val items: ArrayList<PurchaseDetail>,
    private val context: Context
) : RecyclerView.Adapter<PurchasesDetailsAdapter.ViewHolder>() {
    private val numberFormatHelper = NumberFormatHelper()
    class ViewHolder(val binding: RowItemPurchaseDetailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowItemPurchaseDetailBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.title.text = "Producto: #${position + 1}/${itemCount}: ${items[position].productName}"
        holder.binding.sellerName.text = items[position].sellerName
        holder.binding.productPrice.text = "$" + numberFormatHelper.format2Decimals(items[position].productPrice)
        holder.binding.productQuantity.text = items[position].quantity
        holder.binding.summarySubtotal.text = "$" + numberFormatHelper.format2Decimals(items[position].subtotal)
        holder.binding.summaryDiscount.text = "$" + numberFormatHelper.format2Decimals(items[position].discount)
        holder.binding.summaryIva.text = "$" + numberFormatHelper.format2Decimals(items[position].iva)
        holder.binding.summaryTotal.text = "$" + numberFormatHelper.format2Decimals(items[position].total)

        if (items[position].images == null) {
            holder.binding.image.setImageResource(R.drawable.missing_img)
        } else {
            Glide.with(context)
                .load("${context.resources.getString(R.string.api_url_storage)}${items[position].keySeller}/products/${JSONArray(items[position].images).getString(0)}")
                // .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                // .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(holder.binding.image)
        }
    }

    override fun getItemCount(): Int { return items.size }
}
