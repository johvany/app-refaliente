package com.di.refaliente.shared

import com.android.volley.RequestQueue

class Utilities {
    companion object {
        var queue: RequestQueue? = null

        fun showUnconnectedMessage(customAlertDialog: CustomAlertDialog) {
            customAlertDialog.setTitle("Sin conexión")
            customAlertDialog.setMessage("Por favor asegúrate de tener una conexión a internet e intenta de nuevo.")
            customAlertDialog.setErrorDetail(null)
            customAlertDialog.show()
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