package com.di.refaliente.shared

class Utilities {
    companion object {
        fun showUnconnectedMessage(customAlertDialog: CustomAlertDialog) {
            customAlertDialog.setTitle("Sin conexión")
            customAlertDialog.setMessage("Por favor asegúrate de tener una conexión a internet e intenta de nuevo.")
            customAlertDialog.show()
        }

        fun showRequestError(customAlertDialog: CustomAlertDialog, errorDetail: String?) {
            customAlertDialog.setTitle("Operación fallida")
            customAlertDialog.setMessage("No se pudo realizar la operación solicitada. Por favor asegúrate de tener una conexión a internet e intenta de nuevo.")
            customAlertDialog.setErrorDetail(errorDetail)
            customAlertDialog.show()
        }
    }
}