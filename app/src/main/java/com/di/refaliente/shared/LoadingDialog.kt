package com.di.refaliente.shared

import android.content.Context
import android.view.LayoutInflater
import com.di.refaliente.databinding.LoadingDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoadingDialog(context: Context) {
    private val binding = LoadingDialogBinding.inflate(LayoutInflater.from(context))

    private var dialog = MaterialAlertDialogBuilder(context)
        .setCancelable(false)
        .setView(binding.root)
        .create()

    fun setMessage(message: String) { binding.message.text = message }
    fun show() { dialog.show() }
    fun hide() { dialog.dismiss() }
}