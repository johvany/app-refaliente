package com.di.refaliente.view_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.di.refaliente.databinding.RowItemUserTypeBinding
import com.di.refaliente.shared.UserTypeItem

class UserTypeAdapter(
    private val items: ArrayList<UserTypeItem>,
    private val inflater: LayoutInflater
) : BaseAdapter() {
    override fun getView(itemPosition: Int, recycledView: View?, viewGroup: ViewGroup?): View {
        return if (recycledView == null) {
            RowItemUserTypeBinding.inflate(inflater).also { viewBinding ->
                viewBinding.text.text = items[itemPosition].name
            }.root
        } else {
            RowItemUserTypeBinding.bind(recycledView).also { viewBinding ->
                viewBinding.text.text = items[itemPosition].name
            }.root
        }
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