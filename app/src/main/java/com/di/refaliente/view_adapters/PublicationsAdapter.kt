package com.di.refaliente.view_adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.R
import com.di.refaliente.databinding.RowItemPublicationSmallBinding
import com.di.refaliente.shared.NumberFormatHelper
import com.di.refaliente.shared.PublicationSmall
import java.text.DecimalFormat

class PublicationsAdapter(
    private val items: ArrayList<PublicationSmall>,
    private val context: Context,
    private val onClick: (idPublication: Int) -> Unit
) : RecyclerView.Adapter<PublicationsAdapter.ViewHolder>() {
    private val decimalFormat = DecimalFormat("#,###,###,##0.00")
    private val numberFormatHelper = NumberFormatHelper()

    class ViewHolder(val binding: RowItemPublicationSmallBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowItemPublicationSmallBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.publicationTitle.text = items[position].title
        holder.binding.publicationPrice.text = "MXN $" + decimalFormat.format(numberFormatHelper.strToDouble(items[position].price))

        if (items[position].priceOld == null) {
            holder.binding.publicationPriceOld.visibility = View.INVISIBLE
        } else {
            holder.binding.publicationPriceOld.visibility = View.VISIBLE
            holder.binding.publicationPriceOld.text = "MXN $" + decimalFormat.format(numberFormatHelper.strToDouble(items[position].priceOld!!))
            holder.binding.publicationPriceOld.paintFlags = holder.binding.publicationPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        if (items[position].img != null) {
            Glide.with(context)
                .load(context.resources.getString(R.string.api_url_storage) + items[position].keyUserOwner + "/products/" + items[position].img)
                .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(holder.binding.publicationImg)
        }

        holder.binding.publicationImg.setOnClickListener { onClick(items[position].idPublication) }
        holder.binding.publicationTitle.setOnClickListener { onClick(items[position].idPublication) }
        holder.binding.buyProduct.setOnClickListener { onClick(items[position].idPublication) }
    }

    override fun getItemCount(): Int { return items.size }
}
