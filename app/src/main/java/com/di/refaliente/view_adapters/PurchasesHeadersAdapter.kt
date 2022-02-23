package com.di.refaliente.view_adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.di.refaliente.databinding.RowItemPurchaseBinding
import com.di.refaliente.shared.PurchaseHeader

class PurchasesHeadersAdapter(
    private val items: ArrayList<PurchaseHeader>,
    private val context: Context
) : RecyclerView.Adapter<PurchasesHeadersAdapter.ViewHolder>() {

    class ViewHolder(val binding: RowItemPurchaseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RowItemPurchaseBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.idPurchase.text = "Compra ID: " + items[position].idPurchaseFormatted
        holder.binding.createdAtShort.text = items[position].createdAtShort

        if (items[position].customerPhone == null) {
            holder.binding.customerName.text = items[position].customerName
        } else {
            holder.binding.customerName.text = HtmlCompat.fromHtml(
                "${items[position].customerName} (<span style=\"color: #334FA6;\">Tel. ${items[position].customerPhone}</span>)",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        holder.binding.customerEmail.text = items[position].customerEmail
        holder.binding.customerAddress.text = items[position].customerAddress + ", " + items[position].customerPostalCode
    }

    override fun getItemCount(): Int { return items.size }
}
