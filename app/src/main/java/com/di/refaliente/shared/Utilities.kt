package com.di.refaliente.shared

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.android.volley.RequestQueue
import com.di.refaliente.R
import com.di.refaliente.databinding.MyDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Utilities {
    companion object {
        var queue: RequestQueue? = null

        fun showUnconnectedMessage(customAlertDialog: CustomAlertDialog) {
            customAlertDialog.setTitle("Sin conexión")
            customAlertDialog.setMessage("Por favor asegúrate de tener una conexión a internet e intenta de nuevo.")
            customAlertDialog.setErrorDetail(null)
            customAlertDialog.show()
        }

        fun showUnconnectedMessage2(context: Context) {
            MaterialAlertDialogBuilder(context).create().also { dialog ->
                dialog.setCancelable(false)

                dialog.setView(MyDialogBinding.inflate(LayoutInflater.from(context)).also { viewBinding ->
                    viewBinding.icon.setImageResource(R.drawable.info_dialog)
                    viewBinding.title.text = "Sin conexión"
                    viewBinding.message.text = "Por favor asegúrate de tener una conexión a internet e intenta de nuevo."
                    viewBinding.negativeButton.visibility = View.GONE
                    viewBinding.positiveButton.text = "Aceptar"
                    viewBinding.positiveButton.setOnClickListener { dialog.dismiss() }
                }.root)
            }.show()
        }

        fun showRequestError(customAlertDialog: CustomAlertDialog, errorDetail: String?) {
            customAlertDialog.setTitle("Operación fallida")
            customAlertDialog.setMessage("No se pudo realizar la operación solicitada. Por favor asegúrate de tener una conexión a internet e intenta de nuevo.")
            customAlertDialog.setErrorDetail(errorDetail)
            customAlertDialog.show()
        }

        fun showUnknownError(customAlertDialog: CustomAlertDialog, errorDetail: String?) {
            customAlertDialog.setTitle("Operación fallida")
            customAlertDialog.setMessage("Ocurrió un incidente al intentar realizar la operación solicitada. Por favor intenta de nuevo y si el problema continua contacta a soporte.")
            customAlertDialog.setErrorDetail(errorDetail)
            customAlertDialog.show()
        }
    }
}