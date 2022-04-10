package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.di.refaliente.databinding.ActivityLoginBinding
import com.di.refaliente.local_database.Database
import com.di.refaliente.local_database.UsersDetailsTable
import com.di.refaliente.local_database.UsersTable
import com.di.refaliente.shared.*
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private lateinit var db: Database

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customAlertDialog = CustomAlertDialog(this)
        db = Database(this)

        // TODO: Delete this when release a version of this app in production.
        binding.loginIcon.setOnClickListener {
            binding.userEmail.setText("programadorequis@gmail.com")
            binding.userPassword.setText("123123123")
        }

        binding.facebookLoginBtn.setOnClickListener { loginWithFacebook() }
        binding.googleLoginBtn.setOnClickListener { loginWithGoogle() }
        binding.startSession.setOnClickListener { startSession() }

        // Set empty click listener to this view, to prevent the user click others views like
        // when the loadingBackground view is visible buttons.
        binding.loadingBackground.setOnClickListener { /* ... */ }
    }

    private fun loginWithFacebook() {
        Toast.makeText(this, "...Iniciar sesión con Facebook esta en construcción...", Toast.LENGTH_LONG).show()
    }

    private fun loginWithGoogle() {
        Toast.makeText(this, "...Iniciar sesión con Google esta en construcción...", Toast.LENGTH_LONG).show()
    }

    private fun startSession() {
        showLoading()
        resetInputErrors()

        if (validCredentials(binding.userEmail.text.toString(), binding.userPassword.text.toString())) {
            loginStep1(binding.userEmail.text.toString(), binding.userPassword.text.toString())
        } else {
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.loadingBackground.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingBackground.visibility = View.INVISIBLE
    }

    // In this step we get the user information, then call the step 2 function
    // to get the user token.
    private fun loginStep1(email: String, password: String) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.POST,
                resources.getString(R.string.api_url) + "login",
                null,
                { response ->
                    if (response.has("sub")) {
                        loginStep2(response, email, password)
                    } else {
                        customAlertDialog.setTitle("")
                        customAlertDialog.setMessage(response.getString("message"))
                        customAlertDialog.show()
                        hideLoading()
                    }
                },
                { error ->
                    try {
                        Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                    } catch (err: Exception) {
                        Utilities.showRequestError(customAlertDialog, error.toString())
                    }

                    hideLoading()
                }
            ) {
                override fun getBodyContentType(): String {
                    return "application/x-www-form-urlencoded"
                }

                override fun getBody(): ByteArray {
                    return "json=${URLEncoder.encode("{\"email\":\"$email\",\"password\":\"$password\",\"gettoken\":true}", "utf-8")}".toByteArray()
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

    // In this step we get the user token (after step 1 finish).
    private fun loginStep2(userData: JSONObject, email: String, password: String) {
        Utilities.queue?.add(object: StringRequest(
            Method.POST,
            resources.getString(R.string.api_url) + "login",
            { response ->
                try {
                    JSONObject(response).let { jsonObj ->
                        customAlertDialog.setTitle("")
                        customAlertDialog.setMessage(jsonObj.getString("message"))
                        customAlertDialog.show()
                        hideLoading()
                    }
                } catch (err: Exception) {
                    saveUserSession(userData, email, password, response.substring(1, response.length - 1))
                }
            },
            { error ->
                try {
                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }

                hideLoading()
            }
        ) {
            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded"
            }

            override fun getBody(): ByteArray {
                return "json=${URLEncoder.encode("{\"email\":\"$email\",\"password\":\"$password\",\"gettoken\":false}", "utf-8")}".toByteArray()
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun saveUserSession(userData: JSONObject, userEmail: String, userPassword: String, userToken: String) {
        UsersTable.find(db, 1).let { foundUser ->
            if (foundUser == null) {
                UsersTable.create(db, User(
                    1,
                    userData.getInt("sub"),
                    userEmail,
                    userData.getString("name"),
                    userData.getString("surname"),
                    userData.getString("role_user"),
                    userPassword,
                    userToken
                ))
            } else {
                UsersTable.update(db, User(
                    1,
                    userData.getInt("sub"),
                    userEmail,
                    userData.getString("name"),
                    userData.getString("surname"),
                    userData.getString("role_user"),
                    userPassword,
                    userToken
                ))
            }
        }

        val userDetailData = userData.getJSONObject("details")

        UsersDetailsTable.find(db, 1).let { foundUserDetail ->
            if (foundUserDetail == null) {
                UsersDetailsTable.create(db, UserDetail(
                    1,
                    userData.getInt("sub"),
                    userDetailData.getString("name"),
                    userDetailData.getString("surname"),
                    userDetailData.getString("email"),
                    userDetailData.getString("description"),
                    userDetailData.getInt("key_type_user"),
                    userDetailData.getInt("key_business_type"),
                    userDetailData.getString("telephone"),
                    userDetailData.getString("profile_image"),
                    userDetailData.getString("session_type"),
                    userDetailData.getString("rfc"),
                    userDetailData.getString("social_reason"),
                    userDetailData.getString("enterprise_name")
                ))
            } else {
                UsersDetailsTable.update(db, UserDetail(
                    1,
                    userData.getInt("sub"),
                    userDetailData.getString("name"),
                    userDetailData.getString("surname"),
                    userDetailData.getString("email"),
                    if (userDetailData.getString("description") == "null") { null } else { userDetailData.getString("description") },
                    userDetailData.getInt("key_type_user"),
                    userDetailData.getInt("key_business_type"),
                    if (userDetailData.getString("telephone") == "null") { null } else { userDetailData.getString("telephone") },
                    if (userDetailData.getString("profile_image") == "null") { null } else { userDetailData.getString("profile_image") },
                    userDetailData.getString("session_type"),
                    if (userDetailData.getString("rfc") == "null") { null } else { userDetailData.getString("rfc") },
                    if (userDetailData.getString("social_reason") == "null") { null } else { userDetailData.getString("social_reason") },
                    if (userDetailData.getString("enterprise_name") == "null") { null } else { userDetailData.getString("enterprise_name") }
                ))
            }
        }

        startActivity(Intent(this, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun resetInputErrors() {
        binding.userEmailContainer.error = null
        binding.userEmailContainer.isErrorEnabled = false
        binding.userPasswordContainer.error = null
        binding.userPasswordContainer.isErrorEnabled = false
    }

    private fun validCredentials(email: String, password: String): Boolean {
        var validCred = true

        when {
            email == "" -> {
                validCred = false
                binding.userEmailContainer.error = "El campo correo es obligatorio"
                binding.userEmailContainer.isErrorEnabled = true
            }
            !Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}\$", Pattern.UNICODE_CASE).matcher(email).matches() -> {
                validCred = false
                binding.userEmailContainer.error = "El correo no es válido"
                binding.userEmailContainer.isErrorEnabled = true
            }
        }

        if (password == "") {
            validCred = false
            binding.userPasswordContainer.error = "El campo contraseña es obligatorio"
            binding.userPasswordContainer.isErrorEnabled = true
        }

        return validCred
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}