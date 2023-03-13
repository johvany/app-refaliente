package com.di.refaliente.view_adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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

class PublicationsAdapter(
    private val items: ArrayList<PublicationSmall>,
    private val context: Context,
    private val onClick: (idPublication: Int) -> Unit
) : RecyclerView.Adapter<PublicationsAdapter.ViewHolder>() {
    private val numberFormatHelper = NumberFormatHelper()

    class ViewHolder(val binding: RowItemPublicationSmallBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowItemPublicationSmallBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.publicationTitle.text = if (items[position].title.length > 120) { items[position].title.substring(0, 120) + "..." } else { items[position].title }
        holder.binding.publicationPrice.text = "MXN $" + numberFormatHelper.format2Decimals(items[position].price)

        holder.binding.publicationRemateBadge.visibility = if (items[position].hasDiscount == 1) { View.VISIBLE } else { View.GONE }
        holder.binding.publicationNewBadge.visibility = if (items[position].productKeyCondition == 1) { View.VISIBLE } else { View.GONE }
        holder.binding.publicationUsedBadge.visibility = if (items[position].productKeyCondition == 2) { View.VISIBLE } else { View.GONE }
        holder.binding.publicationSeminewBadge.visibility = if (items[position].productKeyCondition == 3) { View.VISIBLE } else { View.GONE }

        if (items[position].priceOld == null) {
            holder.binding.publicationPriceOld.text = ""
            holder.binding.publicationPriceOld.visibility = View.GONE
        } else {
            holder.binding.publicationPriceOld.text = "MXN $" + numberFormatHelper.format2Decimals(items[position].priceOld!!)
            holder.binding.publicationPriceOld.paintFlags = holder.binding.publicationPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.publicationPriceOld.visibility = View.VISIBLE
        }

        if (items[position].img != null) {
            Glide.with(context)
                .load(context.resources.getString(R.string.api_url_storage) + items[position].keyUserOwner + "/products/" + items[position].img)
                .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(holder.binding.publicationImg)
        }

        holder.binding.productQualification.text = items[position].productQualificationAvg

        // Set color to stars
        when (items[position].productQualification) {
            1 -> {
                holder.binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            2 -> {
                holder.binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            3 -> {
                holder.binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            4 -> {
                holder.binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            5 -> {
                holder.binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                holder.binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
            }
            else -> {
                holder.binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                holder.binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
        }

        holder.binding.publicationImg.setOnClickListener { onClick(items[position].idPublication) }
        holder.binding.publicationTitle.setOnClickListener { onClick(items[position].idPublication) }
        holder.binding.buyProduct.setOnClickListener { onClick(items[position].idPublication) }
    }

    override fun getItemCount(): Int { return items.size }
}
