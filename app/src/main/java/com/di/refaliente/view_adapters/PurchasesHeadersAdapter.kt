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
        holder.binding.idPurchase.text = "Compra ID: " + items[position].idPurchase
        holder.binding.customerName.text = HtmlCompat.fromHtml("Alan Thomas Anderson (<span style=\"color: #334FA6;\">Tel. 6666666666</span>)", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    override fun getItemCount(): Int { return items.size }
}
