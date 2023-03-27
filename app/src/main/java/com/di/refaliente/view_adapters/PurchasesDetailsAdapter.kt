package com.di.refaliente.view_adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.di.refaliente.R
import com.di.refaliente.databinding.RowItemPurchaseDetailBinding
import com.di.refaliente.shared.NumberFormatHelper
import com.di.refaliente.shared.PurchaseDetail
import com.di.refaliente.shared.SessionHelper
import org.json.JSONArray

class PurchasesDetailsAdapter(
    private val items: ArrayList<PurchaseDetail>,
    private val context: Context,
    private val saveProductComment: (idSaleDetail: Int, comment: String, qualification: Int) -> Unit
) : RecyclerView.Adapter<PurchasesDetailsAdapter.ViewHolder>() {
    private val numberFormatHelper = NumberFormatHelper()
    class ViewHolder(val binding: RowItemPurchaseDetailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowItemPurchaseDetailBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // ------------------------
        // Load product information
        // ------------------------

        holder.binding.title.text = "Producto: #${position + 1}/${itemCount}: ${items[position].productName}"
        holder.binding.sellerName.text = items[position].sellerName
        holder.binding.productPrice.text = "$" + numberFormatHelper.format2Decimals(items[position].productPrice)
        holder.binding.productQuantity.text = items[position].quantity
        holder.binding.summarySubtotal.text = "$" + numberFormatHelper.format2Decimals(items[position].subtotal)
        holder.binding.summaryDiscount.text = "$" + numberFormatHelper.format2Decimals(items[position].discount)
        holder.binding.summaryIva.text = "$" + numberFormatHelper.format2Decimals(items[position].iva)
        holder.binding.summaryTotal.text = "$" + numberFormatHelper.format2Decimals(items[position].total)

        // ------------------
        // Show image product
        // ------------------

        if (items[position].images == null) {
            holder.binding.image.setImageResource(R.drawable.missing_img)
        } else {
            Glide.with(context)
                .load("${context.resources.getString(R.string.api_url_storage)}${items[position].keySeller}/products/${JSONArray(items[position].images).getString(0)}")
                // .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                // .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                .into(holder.binding.image)
        }

        if (items[position].idComment == null) {
            // -----------------------------------------------------------------
            // Show or hide button used to show or hide comment area (edit mode)
            // -----------------------------------------------------------------

            holder.binding.showOrHideCommentArea.visibility = if (items[position].readyToComment) { View.VISIBLE } else { View.GONE }

            // ------------------------------------------------------------------------------
            // Set click listener to the button used to show or hide comment area (edit mode)
            // ------------------------------------------------------------------------------

            holder.binding.showOrHideCommentArea.setOnClickListener {
                items[position].showCommentArea = !items[position].showCommentArea

                if (items[position].showCommentArea) {
                    holder.binding.commentFieldTitle.visibility = View.VISIBLE
                    holder.binding.commentFieldContainer.visibility = View.VISIBLE
                    holder.binding.commentQualificationContainer.visibility = View.VISIBLE
                    holder.binding.commentSaveButton.visibility = View.VISIBLE
                } else {
                    holder.binding.commentFieldTitle.visibility = View.GONE
                    holder.binding.commentFieldContainer.visibility = View.GONE
                    holder.binding.commentQualificationContainer.visibility = View.GONE
                    holder.binding.commentSaveButton.visibility = View.GONE
                }
            }

            // ------------------------------------------------------
            // Restore the visibility of the comment area (edit mode)
            // ------------------------------------------------------

            if (items[position].showCommentArea) {
                holder.binding.commentFieldTitle.visibility = View.VISIBLE
                holder.binding.commentFieldContainer.visibility = View.VISIBLE
                holder.binding.commentQualificationContainer.visibility = View.VISIBLE
                holder.binding.commentSaveButton.visibility = View.VISIBLE
            } else {
                holder.binding.commentFieldTitle.visibility = View.GONE
                holder.binding.commentFieldContainer.visibility = View.GONE
                holder.binding.commentQualificationContainer.visibility = View.GONE
                holder.binding.commentSaveButton.visibility = View.GONE
            }

            // -------------------------------------------------------------------------
            // Set click listener to qualification stars of the comment area (edit mode)
            // -------------------------------------------------------------------------

            holder.binding.commentQualificationStar1.setOnClickListener {
                items[position].qualificationStars = 1
                holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#D3D3D3"))
                holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#D3D3D3"))
                holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
            }

            holder.binding.commentQualificationStar2.setOnClickListener {
                items[position].qualificationStars = 2
                holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#D3D3D3"))
                holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
            }

            holder.binding.commentQualificationStar3.setOnClickListener {
                items[position].qualificationStars = 3
                holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
            }

            holder.binding.commentQualificationStar4.setOnClickListener {
                items[position].qualificationStars = 4
                holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
            }

            holder.binding.commentQualificationStar5.setOnClickListener {
                items[position].qualificationStars = 5
                holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#EEE000"))
                holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#EEE000"))
            }

            // -----------------------------------------------------------
            // Restore qualification stars of the comment area (edit mode)
            // -----------------------------------------------------------

            when (items[position].qualificationStars) {
                1 -> {
                    holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                2 -> {
                    holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                3 -> {
                    holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                4 -> {
                    holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                5 -> {
                    holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#EEE000"))
                }
                else -> {
                    holder.binding.commentQualificationStar1.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar2.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar3.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualificationStar5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
            }

            // ---------------------------------------------
            // Set click listener to the save comment button
            // ---------------------------------------------

            holder.binding.commentSaveButton.setOnClickListener {
                saveProductComment(
                    items[position].idSaleDetail,
                    if (holder.binding.commentField.text.isNullOrBlank()) { "" } else { holder.binding.commentField.text.toString() },
                    items[position].qualificationStars
                )
            }

            // -----------------------------
            // Hide comment area (read mode)
            // -----------------------------

            holder.binding.commentQualification2Container.visibility = View.GONE
            holder.binding.comment.visibility = View.GONE
            holder.binding.commentDivider.visibility = View.GONE
            holder.binding.commentDate.visibility = View.GONE
        } else {
            // -----------------------------
            // Hide comment area (edit mode)
            // -----------------------------

            holder.binding.showOrHideCommentArea.visibility = View.GONE
            holder.binding.commentFieldTitle.visibility = View.GONE
            holder.binding.commentFieldContainer.visibility = View.GONE
            holder.binding.commentQualificationContainer.visibility = View.GONE
            holder.binding.commentSaveButton.visibility = View.GONE

            // ---------------------------------------------------------------
            // Restore the qualification stars of the comment area (read mode)
            // ---------------------------------------------------------------

            when (items[position].commentQualification) {
                1 -> {
                    holder.binding.commentQualification2Star1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star2.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star3.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                2 -> {
                    holder.binding.commentQualification2Star1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star3.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                3 -> {
                    holder.binding.commentQualification2Star1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star3.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                4 -> {
                    holder.binding.commentQualification2Star1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star3.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star4.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
                5 -> {
                    holder.binding.commentQualification2Star1.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star2.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star3.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star4.setColorFilter(Color.parseColor("#EEE000"))
                    holder.binding.commentQualification2Star5.setColorFilter(Color.parseColor("#EEE000"))
                }
                else -> {
                    holder.binding.commentQualification2Star1.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star2.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star3.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star4.setColorFilter(Color.parseColor("#D3D3D3"))
                    holder.binding.commentQualification2Star5.setColorFilter(Color.parseColor("#D3D3D3"))
                }
            }

            // -------------------------------------------------
            // Load comment data in the comment area (read mode)
            // -------------------------------------------------

            holder.binding.comment.text = items[position].comment
            holder.binding.commentDate.text = SessionHelper.user!!.name + " - " + items[position].commentDate

            // -----------------------------
            // Show comment area (read mode)
            // -----------------------------

            holder.binding.commentQualification2Container.visibility = View.VISIBLE
            holder.binding.comment.visibility = View.VISIBLE
            holder.binding.commentDivider.visibility = View.VISIBLE
            holder.binding.commentDate.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int { return items.size }
}
