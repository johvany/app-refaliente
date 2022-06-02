package com.di.refaliente.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.di.refaliente.databinding.RowItemSimpleTextBinding
import com.di.refaliente.shared.UserTypeItem

class UserTypesAdapter(
    private val items: ArrayList<UserTypeItem>,
    private val inflater: LayoutInflater
) : BaseAdapter() {
    override fun getView(itemPosition: Int, recycledView: View?, viewGroup: ViewGroup?): View {
        val viewBinding = if (recycledView == null) { RowItemSimpleTextBinding.inflate(inflater) } else { RowItemSimpleTextBinding.bind(recycledView) }
        viewBinding.singleItem.text = items[itemPosition].name
        return viewBinding.root
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(itemPosition: Int): Any {
        return items[itemPosition]
    }

    override fun getItemId(itemPosition: Int): Long {
        return 0L
    }
}