package com.di.refaliente.shared

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.di.refaliente.databinding.CustomAlertDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@SuppressLint("SetTextI18n")
class CustomAlertDialog(context: Context) {
    private val binding = CustomAlertDialogBinding.inflate(LayoutInflater.from(context))

    private var dialog = MaterialAlertDialogBuilder(context)
        .setCancelable(false)
        .setView(binding.root)
        .setPositiveButton("ACEPTAR", null)
        .create()

    init {
        binding.errorDetailContainer.visibility = View.GONE
        binding.toggleErrorDetail.visibility = View.GONE

        binding.toggleErrorDetail.setOnClickListener {
            if (binding.errorDetailContainer.visibility == View.GONE) {
                binding.errorDetailContainer.visibility = View.VISIBLE
                binding.toggleErrorDetail.text = "OCULTAR DETALLE"
            } else {
                binding.errorDetailContainer.visibility = View.GONE
                binding.toggleErrorDetail.text = "MOSTRAR DETALLE"
            }
        }

        binding.errorDetail.setOnClickListener {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.let {
                it.setPrimaryClip(ClipData.newPlainText("Error detail", binding.errorDetail.text))
                Toast.makeText(context, "Texto copiado", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setTitle(title: String) { dialog.setTitle(title) }
    fun setMessage(message: String) { binding.message.text = message }
    fun show() { dialog.show() }
    fun dismiss() { dialog.dismiss() }

    fun setErrorDetail(message: String?) {
        binding.errorDetailContainer.visibility = View.GONE

        if (message == null) {
            binding.toggleErrorDetail.visibility = View.GONE
        } else {
            binding.errorDetail.text = message
            binding.toggleErrorDetail.visibility = View.VISIBLE
            binding.toggleErrorDetail.text = "MOSTRAR DETALLE"
        }
    }
}