package com.di.refaliente.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.di.refaliente.databinding.RowItemCardMonthBinding
import com.di.refaliente.shared.CardMonth

class CardMonthsAdapter(
    private val items: ArrayList<CardMonth>,
    private val inflater: LayoutInflater
) : BaseAdapter() {
    override fun getCount(): Int { return items.size }
    override fun getItem(itemPosition: Int): Any { return items[itemPosition] }
    override fun getItemId(itemPosition: Int): Long { return 0L }

    override fun getView(itemPosition: Int, recycledView: View?, viewGroup: ViewGroup?): View {
        return if (recycledView == null) {
            RowItemCardMonthBinding.inflate(inflater).also { view ->
                view.monthValue.text = items[itemPosition].value
            }.root
        } else {
            RowItemCardMonthBinding.bind(recycledView).also { view ->
                view.monthValue.text = items[itemPosition].value
            }.root
        }
    }
}