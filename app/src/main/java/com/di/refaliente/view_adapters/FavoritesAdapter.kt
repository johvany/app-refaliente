package com.di.refaliente.view_adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.R
import com.di.refaliente.databinding.RowItemFavoriteBinding
import com.di.refaliente.shared.NumberFormatHelper
import com.di.refaliente.shared.Publication
import org.json.JSONArray

class FavoritesAdapter(
    private val items: ArrayList<Publication>,
    private val context: Context,
    private val onProductImgClick: (itemPosition: Int) -> Unit,
    private val onRemoveFromFavoritesClick: (itemPosition: Int) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
    private val numberFormatHelper = NumberFormatHelper()

    class ViewHolder(val binding: RowItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowItemFavoriteBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.publicationTitle.text = items[position].title
        holder.binding.publicationRemateBadge.visibility = if (items[position].hasDiscount == 1) { View.VISIBLE } else { View.GONE }
        holder.binding.publicationNewBadge.visibility = if (items[position].product.keyCondition == 1) { View.VISIBLE } else { View.GONE }
        holder.binding.publicationUsedBadge.visibility = if (items[position].product.keyCondition == 2) { View.VISIBLE } else { View.GONE }
        holder.binding.publicationSeminewBadge.visibility = if (items[position].product.keyCondition == 3) { View.VISIBLE } else { View.GONE }
        holder.binding.productPreviousPrice.text = HtmlCompat.fromHtml("<strike>$ ${numberFormatHelper.format2Decimals(items[position].product.previousPrice ?: "0")} MXN</strike>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.binding.productPreviousPrice.visibility = if (items[position].hasDiscount == 1) { View.VISIBLE } else { View.GONE }
        holder.binding.productPrice.text = "$ ${numberFormatHelper.format2Decimals(items[position].productPrice)} MXN"
        holder.binding.productDiscountPercent.text = "- ${items[position].discountPercent}% de descuento"
        holder.binding.productDiscountPercent.visibility = if (items[position].product.hasDiscount == 1) { View.VISIBLE } else { View.GONE }
        holder.binding.productQualification.text = items[position].product.qualificationAvg
        holder.binding.productDescription.text = items[position].description

        holder.binding.showDescription.setOnClickListener {
            holder.binding.showDescription.visibility = View.INVISIBLE
            holder.binding.hideDescription.visibility = View.VISIBLE
            holder.binding.productDescription.visibility = View.VISIBLE
        }

        holder.binding.hideDescription.setOnClickListener {
            holder.binding.hideDescription.visibility = View.GONE
            holder.binding.productDescription.visibility = View.GONE
            holder.binding.showDescription.visibility = View.VISIBLE
        }

        holder.binding.publicationTitle.setOnClickListener {
            onProductImgClick(position)
        }

        holder.binding.productImg.setOnClickListener {
            onProductImgClick(position)
        }

        holder.binding.removeFromFavorites.setOnClickListener {
            onRemoveFromFavoritesClick(position)
        }

        if (items[position].product.images != null) {
            try {
                val productImgs = JSONArray(items[position].product.images)
                val imgURL = context.resources.getString(R.string.api_url_storage) + "${items[position].keyUser}/products/${productImgs.getString(0)}"

                Glide.with(context)
                    .load(imgURL)
                    .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                    .into(holder.binding.productImg)
            } catch (err: Exception) {
                // ...
            }
        }

        // Set color to stars
        when (items[position].product.qualification) {
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
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
