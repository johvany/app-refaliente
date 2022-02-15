package com.di.refaliente.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.di.refaliente.databinding.RowItemSimpleAddressBinding
import com.di.refaliente.shared.SimpleAddress

class SimpleAddressAdapter(
    private val items: ArrayList<SimpleAddress>,
    private val inflater: LayoutInflater
) : BaseAdapter() {
    override fun getCount(): Int { return items.size }
    override fun getItem(itemPosition: Int): Any { return items[itemPosition] }
    override fun getItemId(itemPosition: Int): Long { return 0L }

    override fun getView(itemPosition: Int, recycledView: View?, viewGroup: ViewGroup?): View {
        return if (recycledView == null) {
            RowItemSimpleAddressBinding.inflate(inflater).also { view ->
                view.simpleAddressName.text = items[itemPosition].name
            }.root
        } else {
            RowItemSimpleAddressBinding.bind(recycledView).also { view ->
                view.simpleAddressName.text = items[itemPosition].name
            }.root
        }
    }
}