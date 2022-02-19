package com.di.refaliente.shared

import android.content.Context
import android.content.Intent
import com.android.volley.DefaultRetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.di.refaliente.HomeMenuActivity
import com.di.refaliente.LoginActivity
import com.di.refaliente.R
import com.di.refaliente.local_database.Database
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
            Database(context).use { db ->
                UsersTable.update(db, User(
                    1,
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                ))

                UsersDetailsTable.update(db, UserDetail(
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
                ))
            }

            context.startActivity(Intent(context, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }

        // This function will try to login again to get a new user token, if all is ok, the new user
        // token will be saved in the local database and the "user" var in this class will be updated too.
        private fun refreshUserToken(context: Context, success: Runnable, fail: Runnable, userAccountDisabled: Runnable) {
            if (user == null) {
                fail.run()
            } else {
                Volley.newRequestQueue(context).add(object: StringRequest(
                    Method.POST,
                    context.resources.getString(R.string.api_url) + "login",
                    { response ->
                        try {
                            JSONObject(response).let { resp ->
                                if (resp.has("error_code") && resp.getInt("error_code") == 2) {
                                    userAccountDisabled.run()
                                } else {
                                    fail.run()
                                }
                            }
                        } catch (err: Exception) {
                            Database(context).use { db ->
                                User(
                                    user!!.idLocal,
                                    user!!.sub,
                                    user!!.email,
                                    user!!.name,
                                    user!!.surname,
                                    user!!.roleUser,
                                    user!!.password,
                                    response.substring(1, response.length - 1)
                                ).let { updatedUser ->
                                    user = updatedUser
                                    UsersTable.update(db, updatedUser)
                                }
                            }

                            success.run()
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
                        return "json=${URLEncoder.encode("{\"email\":\"${user!!.email}\",\"password\":\"${user!!.password}\",\"gettoken\":false}", "utf-8")}".toByteArray()
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

        // The main purpose of this function is check if there is a token or account status error.
        // The token error means it was expired and its necessary to get one new.
        // The account status error happens when the user account has been disabled.
        fun handleRequestError(error: VolleyError, context: Context, customAlertDialog: CustomAlertDialog, sendRequestAgain: Runnable) {
            try {
                JSONObject(error.networkResponse.data.decodeToString()).let { errorResponse ->
                    if (errorResponse.has("error_token")) {
                        when (errorResponse.getInt("error_token")) {
                            TOKEN_EXPIRED -> {
                                refreshUserToken(
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

                                        MaterialAlertDialogBuilder(context)
                                            .setTitle("Cuenta deshabilitada")
                                            .setMessage("Su cuenta ha sido deshabilitada y actualmente no puede usarla.")
                                            .setCancelable(false)
                                            .setPositiveButton("ACEPTAR") { _, _ -> logout(context) }
                                            .show()
                                    }
                                )
                            }
                            ACCOUNT_DISABLED -> {
                                // The active session will be closed.
                                MaterialAlertDialogBuilder(context)
                                    .setTitle("Cuenta deshabilitada")
                                    .setMessage("Su cuenta ha sido deshabilitada y actualmente no puede usarla.")
                                    .setCancelable(false)
                                    .setPositiveButton("ACEPTAR") { _, _ -> logout(context) }
                                    .show()
                            }
                        }
                    }
                }
            } catch (err: Exception) {
                Utilities.showRequestError(customAlertDialog, null)
            }
        }
    }
}