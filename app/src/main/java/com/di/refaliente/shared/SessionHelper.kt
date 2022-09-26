package com.di.refaliente.shared

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import com.android.volley.DefaultRetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.di.refaliente.HomeMenuActivity
import com.di.refaliente.LoginActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.local_database.Database
import com.di.refaliente.local_database.SessionsAuxTable
import com.di.refaliente.local_database.UsersDetailsTable
import com.di.refaliente.local_database.UsersTable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Runnable
import org.json.JSONObject
import java.net.URLEncoder

class SessionHelper {
    companion object {
        // IMPORTANT
        // Don't change this values unless the backend change.
        private const val TOKEN_EXPIRED = 1
        private const val ACCOUNT_DISABLED = 2
        private const val SESSION_NOT_VALID = 3

        var user: User? = null

        fun userLogged(): Boolean {
            return when {
                user == null -> { false }
                user!!.sub == 0 -> { false }
                else -> { true }
            }
        }

        // Show the login screen.
        fun login(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }

        // Update the user data in the local database.
        // This will update "users" and "users_details" tables. Then will launch the main activity
        // (HomeMenuActivity) and finish all others activitys.
        fun logout(context: Context) {
            val sessionAuxItem: SessionAux?

            Database(context).use { db ->
                val userDetail = UserDetail(
                    1,
                    0,
                    "",
                    "",
                    "",
                    null,
                    0,
                    0,
                    null,
                    null,
                    "",
                    null,
                    null,
                    null
                )

                UsersTable.update(db, User(
                    1,
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    userDetail
                ))

                UsersDetailsTable.update(db, userDetail)
                sessionAuxItem = SessionsAuxTable.find(db, user!!.sub)
            }

            if (sessionAuxItem == null) {
                context.startActivity(Intent(context, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            } else {
                Volley.newRequestQueue(context).add(object: JsonObjectRequest(
                    Method.GET,
                    context.resources.getString(R.string.api_url) + "logout?id_user=" + sessionAuxItem.idUser + "&token_id=" + sessionAuxItem.tokenId,
                    null,
                    { response ->
                        Database(context).use { db -> SessionsAuxTable.update(db, SessionAux(response.getInt("id_user"), response.getInt("token_id"))) }
                        context.startActivity(Intent(context, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    },
                    { context.startActivity(Intent(context, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)) }
                ) { /* ... */ }.apply {
                    retryPolicy = DefaultRetryPolicy(
                        ConstantValues.REQUEST_TIMEOUT,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                })
            }
        }

        // This function will try to login again to get a new user token, if all is ok, the new user
        // token will be saved in the local database and the "user" var in this class will be updated too.
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        private fun refreshUserTokenStep1(context: Context, success: Runnable, fail: Runnable, userAccountDisabled: Runnable, sessionNotValid: Runnable, closeOtherSessions: String) {
            if (user == null) {
                fail.run()
            } else {
                Volley.newRequestQueue(context).add(object: StringRequest(
                    Method.POST,
                    context.resources.getString(R.string.api_url) + "login",
                    { response ->
                        try {
                            JSONObject(response).let { resp ->
                                if (resp.has("error_code")) {
                                    when (resp.getInt("error_code")) {
                                        1 -> {
                                            // May be the password is wrong (because the user login in another device by
                                            // using social network, which means the password was changed automatically by the backend).
                                            sessionNotValid.run()
                                        }
                                        2 -> {
                                            // Disabled account
                                            userAccountDisabled.run()
                                        }
                                        4 -> {
                                            // The backend is requesting the user authorize close other active sessions.
                                            Database(context).use { db ->
                                                val sessionAuxItem = SessionsAuxTable.find(db, resp.getInt("id_user"))

                                                if (sessionAuxItem != null && sessionAuxItem.tokenId == resp.getInt("token_id")) {
                                                    refreshUserTokenStep1(context, success, fail, userAccountDisabled, sessionNotValid, "1")
                                                } else {
                                                    sessionNotValid.run()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    fail.run()
                                }
                            }
                        } catch (err: Exception) {
                            refreshUserTokenStep2(context, success, fail, response.substring(1, response.length - 1))
                        }
                    },
                    {
                        fail.run()
                    }
                ) {
                    override fun getBodyContentType(): String {
                        return "application/x-www-form-urlencoded"
                    }

                    override fun getBody(): ByteArray {
                        return (
                                "json=" + URLEncoder.encode(
                                    JSONObject()
                                        .put("email", user!!.email)
                                        .put("password", user!!.password)
                                        .put("gettoken", false)
                                        .put("session_from", "app")
                                        .put("close_other_sessions", closeOtherSessions)
                                        .toString(),
                                    "UTF-8"
                                )).toByteArray()
                    }
                }.apply {
                    retryPolicy = DefaultRetryPolicy(
                        ConstantValues.REQUEST_TIMEOUT,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )
                })
            }
        }

        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        private fun refreshUserTokenStep2(context: Context, success: Runnable, fail: Runnable, newToken: String) {
            Volley.newRequestQueue(context).add(object: JsonObjectRequest(
                Method.POST,
                context.resources.getString(R.string.api_url) + "login",
                null,
                { response ->
                    Database(context).use { db ->
                        User(
                            user!!.idLocal,
                            user!!.sub,
                            user!!.email,
                            user!!.name,
                            user!!.surname,
                            user!!.roleUser,
                            user!!.password,
                            newToken,
                            user!!.userDetail
                        ).let { updatedUser ->
                            user = updatedUser
                            UsersTable.update(db, updatedUser)
                        }

                        val sessionAuxItem = SessionsAuxTable.find(db, response.getInt("sub"))

                        if (sessionAuxItem == null) {
                            SessionsAuxTable.insert(db, SessionAux(response.getInt("sub"), response.getInt("token_id")))
                        } else {
                            SessionsAuxTable.update(db, SessionAux(response.getInt("sub"), response.getInt("token_id")))
                        }
                    }

                    success.run()
                },
                { error ->
                    fail.run()
                }
            ) {
                override fun getBodyContentType(): String {
                    return "application/x-www-form-urlencoded"
                }

                override fun getBody(): ByteArray {
                    return (
                            "json=" + URLEncoder.encode(
                                JSONObject()
                                    .put("email", user!!.email)
                                    .put("password", user!!.password)
                                    .put("gettoken", true)
                                    .put("session_from", "app")
                                    .put("close_other_sessions", "1")
                                    .toString(),
                                "UTF-8"
                            )).toByteArray()
                }
            }.apply {
                retryPolicy = DefaultRetryPolicy(
                    ConstantValues.REQUEST_TIMEOUT,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            })
        }

        // The main purpose of this function is check if there is a token or account status error.
        // The token error means it was expired and its necessary to get one new.
        // The account status error happens when the user account has been disabled.
        fun handleRequestError(error: VolleyError, context: Context, customAlertDialog: CustomAlertDialog, sendRequestAgain: Runnable) {
            try {
                JSONObject(error.networkResponse.data.decodeToString()).let { errorResponse ->
                    if (errorResponse.has("error_token")) {
                        when (errorResponse.getInt("error_token")) {
                            TOKEN_EXPIRED -> {
                                refreshUserTokenStep1(
                                    context,
                                    {
                                        // At this point the token was successfully refreshed.
                                        // Try send the request again.

                                        sendRequestAgain.run()
                                    },
                                    {
                                        // At this point the token couldn't refreshed.
                                        // Send request again to try refresh token again.

                                        sendRequestAgain.run()
                                    },
                                    {
                                        // At this point the token couldn't refreshed because the user account was disabled.
                                        // The active session will close.

                                        MaterialAlertDialogBuilder(context).create().also { dialog ->
                                            dialog.setCancelable(false)

                                            dialog.setView(MyDialogBinding.inflate(LayoutInflater.from(context)).also { viewBinding ->
                                                viewBinding.icon.setImageResource(R.drawable.info_dialog)
                                                viewBinding.title.text = "¡Cuenta deshabilitada!"
                                                viewBinding.message.text = "Su cuenta ha sido deshabilitada y actualmente no puede usarla"
                                                viewBinding.negativeButton.visibility = View.GONE
                                                viewBinding.positiveButton.text = "Aceptar"

                                                viewBinding.positiveButton.setOnClickListener {
                                                    dialog.dismiss()
                                                    logout(context)
                                                }
                                            }.root)
                                        }.show()
                                    },
                                    {
                                        // At this point, the current user session is not available because the user login
                                        // in another device, and only one session per user is permitted.
                                        // The active session will close.

                                        MaterialAlertDialogBuilder(context).create().also { dialog ->
                                            dialog.setCancelable(false)

                                            dialog.setView(MyDialogBinding.inflate(LayoutInflater.from(context)).also { viewBinding ->
                                                viewBinding.icon.setImageResource(R.drawable.info_dialog)
                                                viewBinding.title.text = "¡Sesión no valida!"
                                                viewBinding.message.text = "Ya tiene una sesión activa en otro dispositivo por lo que esta sesión ya no es válida"
                                                viewBinding.negativeButton.visibility = View.GONE
                                                viewBinding.positiveButton.text = "Aceptar"

                                                viewBinding.positiveButton.setOnClickListener {
                                                    dialog.dismiss()
                                                    logout(context)
                                                }
                                            }.root)
                                        }.show()
                                    },
                                    "0"
                                )
                            }
                            ACCOUNT_DISABLED -> {
                                // The active session will be closed.

                                MaterialAlertDialogBuilder(context).create().also { dialog ->
                                    dialog.setCancelable(false)

                                    dialog.setView(MyDialogBinding.inflate(LayoutInflater.from(context)).also { viewBinding ->
                                        viewBinding.icon.setImageResource(R.drawable.info_dialog)
                                        viewBinding.title.text = "¡Cuenta deshabilitada!"
                                        viewBinding.message.text = "Su cuenta ha sido deshabilitada y actualmente no puede usarla"
                                        viewBinding.negativeButton.visibility = View.GONE
                                        viewBinding.positiveButton.text = "Aceptar"

                                        viewBinding.positiveButton.setOnClickListener {
                                            dialog.dismiss()
                                            logout(context)
                                        }
                                    }.root)
                                }.show()
                            }
                            SESSION_NOT_VALID -> {
                                MaterialAlertDialogBuilder(context).create().also { dialog ->
                                    dialog.setCancelable(false)

                                    dialog.setView(MyDialogBinding.inflate(LayoutInflater.from(context)).also { viewBinding ->
                                        viewBinding.icon.setImageResource(R.drawable.info_dialog)
                                        viewBinding.title.text = "¡Sesión no valida!"
                                        viewBinding.message.text = "Ya tiene una sesión activa en otro dispositivo por lo que esta sesión ya no es válida"
                                        viewBinding.negativeButton.visibility = View.GONE
                                        viewBinding.positiveButton.text = "Aceptar"

                                        viewBinding.positiveButton.setOnClickListener {
                                            dialog.dismiss()
                                            logout(context)
                                        }
                                    }.root)
                                }.show()
                            }
                        }
                    }
                }
            } catch (err: Exception) {
                Utilities.showRequestError(customAlertDialog, null)
            }
        }

        fun showRequiredSessionMessage(context: Context) {
            MaterialAlertDialogBuilder(context)
                .setTitle("Inicio de sesión requerido")
                .setMessage("Necesita iniciar sesión para poder usar esta funcionalidad.")
                .setCancelable(true)
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("INICIAR SESIÓN") { _, _ -> login(context) }
                .show()
        }
    }
}