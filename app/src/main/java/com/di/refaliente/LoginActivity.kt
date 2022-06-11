package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.di.refaliente.databinding.ActivityLoginBinding
import com.di.refaliente.databinding.DialogSocialNetworkRegisterBinding
import com.di.refaliente.local_database.Database
import com.di.refaliente.local_database.UsersDetailsTable
import com.di.refaliente.local_database.UsersTable
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.UserTypesAdapter
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private lateinit var db: Database
    private lateinit var googleSigninLauncher: ActivityResultLauncher<Intent>
    private lateinit var myCallbackManager: CallbackManager

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FacebookSdk.setApplicationId("1163870464433392")
        FacebookSdk.setClientToken("5b3e009d137fac5f0c02261f85c6bab0")
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        myCallbackManager = CallbackManager.Factory.create()

        googleSigninLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            activityResult.data?.let { data ->
                try {
                    val completedTask = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = completedTask.getResult(ApiException::class.java)
                    val userEmail = account.email
                    val userGoogleToken = account.idToken
                    val userName = account.givenName
                    val userLastName = account.familyName

                    if (
                        userEmail != null &&
                        userGoogleToken != null &&
                        userName != null &&
                        userLastName != null
                    ) {
                        tryToLoginWithGoogle(userGoogleToken, userName, userLastName, userEmail)
                    }
                } catch (e: ApiException) {
                    // The ApiException status code indicates the detailed failure reason.
                    // Please refer to the GoogleSignInStatusCodes class reference for more information.

                    // Here we can handle the main status codes.
                    when (e.statusCode) {
                        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                            // ...
                        }
                        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> {
                            // ...
                        }
                        GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                            // ...
                        }
                        GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> {
                            // ...
                        }
                        GoogleSignInStatusCodes.NETWORK_ERROR -> {
                            // ...
                        }
                        GoogleSignInStatusCodes.INVALID_ACCOUNT -> {
                            // ...
                        }
                        GoogleSignInStatusCodes.INTERNAL_ERROR -> {
                            // ...
                        }
                    }
                }
            }
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        customAlertDialog = CustomAlertDialog(this)
        db = Database(this)
        binding.facebookLoginBtn.setOnClickListener { loginWithFacebook() }
        binding.googleLoginBtn.setOnClickListener { loginWithGoogle() }
        binding.startSession.setOnClickListener { startSession() }

        // Set empty click listener to this view, to prevent the user click others views like
        // when the loadingBackground view is visible buttons.
        binding.loadingBackground.setOnClickListener { /* ... */ }
    }

    private fun loginWithFacebook() {
        /* LoginManager.getInstance().let { loginManager ->
            loginManager.registerCallback(myCallbackManager, object: FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    Toast.makeText(this@LoginActivity, "loginWithFacebook > onSuccess", Toast.LENGTH_LONG).show()
                }

                override fun onCancel() {
                    Toast.makeText(this@LoginActivity, "loginWithFacebook > onCancel", Toast.LENGTH_LONG).show()
                }

                override fun onError(error: FacebookException?) {
                    Toast.makeText(this@LoginActivity, "loginWithFacebook > onError", Toast.LENGTH_LONG).show()
                }
            })

            loginManager.logOut()
            loginManager.logInWithReadPermissions(this, listOf("email"))
        } */

        Toast.makeText(this, "...Iniciar sesión con Facebook esta en construcción...", Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        myCallbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun tryToLoginWithGoogle(userGoogleToken: String, userName: String, userLastName: String, userEmail: String) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "login-with-google?token=" + userGoogleToken,
            null,
            { response ->
                saveUserSession(response.getJSONObject("user_data"), userEmail, userGoogleToken, response.getString("token"))
            },
            { error ->
                try {
                    val responseError = JSONObject(error.networkResponse.data.decodeToString())

                    if (responseError.has("user_missing")) {
                        registerUserStep1(userName, userLastName, userEmail, userGoogleToken, "g")
                    } else if (responseError.has("disabled_account")) {
                        customAlertDialog.setTitle("")
                        customAlertDialog.setMessage("La cuenta del usuario esta deshabilitada")
                        customAlertDialog.show()
                    }
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }
            }
        ) {
            // Set request headers here if you need.
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun loginWithGoogle() {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            // Configure sign-in to request the user's ID, email address, and basic
            // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
            // Configure sign-in to request the user's ID, email address, and basic
            // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(resources.getString(R.string.google_oauth_client_id))
                .requestEmail()
                .build()

            // Build a GoogleSignInClient with the options specified by gso.
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

            // Always logout, this way the google API will show to the user a window
            // to choose a google account.
            mGoogleSignInClient.signOut().addOnCompleteListener {
                googleSigninLauncher.launch(mGoogleSignInClient.signInIntent)
            }
        }
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
    @Suppress("SameParameterValue")
    private fun loginStep1(email: String, password: String) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.POST,
                resources.getString(R.string.api_url) + "login",
                null,
                { response ->
                    if (response.has("status") && response.getString("status") == "error") {
                        if (response.has("error_code")) {
                            when (response.getInt("error_code")) {
                                // User not found
                                1 -> {
                                    // ...
                                }
                                // Disabled account
                                2 -> { //
                                    customAlertDialog.setTitle("")
                                    customAlertDialog.setMessage(response.getString("message"))
                                    customAlertDialog.show()
                                    hideLoading()
                                }
                                // Waiting the user activate their account
                                3 -> {
                                    customAlertDialog.setTitle("")
                                    customAlertDialog.setMessage(response.getString("message"))
                                    customAlertDialog.show()
                                    hideLoading()
                                }
                            }
                        } else {
                            customAlertDialog.setTitle("")
                            customAlertDialog.setMessage(response.getString("message"))
                            customAlertDialog.show()
                            hideLoading()
                        }
                    } else {
                        if (response.has("sub")) {
                            loginStep2(response, email, password)
                        } else {
                            customAlertDialog.setTitle("")
                            customAlertDialog.setMessage(response.getString("message"))
                            customAlertDialog.show()
                            hideLoading()
                        }
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

    // In this step we get user types.
    @Suppress("SameParameterValue")
    private fun registerUserStep1(
        userName: String,
        userLastName: String,
        userEmail: String,
        userPassword: String,
        sessionType: String
    ) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "users/types",
            null,
            { response ->
                registerUserStep2(response, userName, userLastName, userEmail, userPassword, sessionType)
            },
            { error ->
                try {
                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }
            }
        ) {
            // Put here request headers if you need.
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    // In this step we show to the user for choose a user type and a enterprise name.
    private fun registerUserStep2(
        response: JSONObject,
        userName: String,
        userLastName: String,
        userEmail: String,
        userPassword: String,
        sessionType: String
    ) {
        val userTypesData = response.getJSONArray("user_types")
        var userTypeJSONItem: JSONObject
        val limit = userTypesData.length()

        if (limit > 0) {
            val userTypes = ArrayList<UserTypeItem>()

            for (i in 0 until limit) {
                userTypeJSONItem = userTypesData.getJSONObject(i)

                userTypes.add(UserTypeItem(
                    userTypeJSONItem.getInt("id_user_type"),
                    userTypeJSONItem.getString("name")
                ))
            }

            val alertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Finalizar registro")
                .setCancelable(false)
                .create()

            val dialogBinding = DialogSocialNetworkRegisterBinding.inflate(layoutInflater)
            dialogBinding.userTypes.adapter = UserTypesAdapter(userTypes, layoutInflater)
            dialogBinding.enterpriseNameContainer.visibility = if ((dialogBinding.userTypes.selectedItem as UserTypeItem).idUserType == 2) { View.VISIBLE } else { View.GONE }

            dialogBinding.userTypes.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if ((dialogBinding.userTypes.selectedItem as UserTypeItem).idUserType == 2) {
                        dialogBinding.enterpriseNameError.visibility = View.GONE
                        dialogBinding.enterpriseName.setText("")
                        dialogBinding.enterpriseName.setBackgroundResource(R.drawable.borders_lightgray)
                        dialogBinding.enterpriseNameContainer.visibility = View.VISIBLE
                    } else {
                        dialogBinding.enterpriseNameContainer.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // ...
                }
            }

            dialogBinding.successBtn.setOnClickListener {
                val keyUserType = (dialogBinding.userTypes.selectedItem as UserTypeItem).idUserType

                if (keyUserType == 2) {
                    val enterpriseName = dialogBinding.enterpriseName.text.toString()

                    if (enterpriseName == "") {
                        dialogBinding.enterpriseName.setBackgroundResource(R.drawable.borders_error)
                        dialogBinding.enterpriseNameError.visibility = View.VISIBLE
                    } else {
                        // register user (enterprise)
                        registerUserStep3(
                            userName,
                            userLastName,
                            userEmail,
                            userPassword,
                            keyUserType,
                            enterpriseName,
                            sessionType,
                            alertDialog
                        )
                    }
                } else {
                    // register user (standard)
                    registerUserStep3(
                        userName,
                        userLastName,
                        userEmail,
                        userPassword,
                        keyUserType,
                        null,
                        sessionType,
                        alertDialog
                    )
                }
            }

            dialogBinding.cancelBtn.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.setView(dialogBinding.root)
            alertDialog.show()
        }
    }

    // In this step we finally collect all data and register the new user.
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun registerUserStep3(
        userName: String,
        userLastName: String,
        userEmail: String,
        userPassword: String,
        keyTypeUser: Int,
        enterpriseName: String?,
        sessionType: String,
        alertDialog: AlertDialog
    ) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            alertDialog.dismiss()
            showLoading()

            // name
            // surname
            // email
            // password
            // key_type_user // 1 = vendedor | 2 = empresa
            // key_business_type // default 1
            // enterprise_name // nullable
            // session_type // e = email | f = facebook | g = google

            val data = JSONObject()
                .put("name", userName)
                .put("surname", userLastName)
                .put("email", userEmail)
                .put("password", userPassword)
                .put("key_type_user", keyTypeUser)
                .put("key_business_type", 1)
                .put("enterprise_name", enterpriseName ?: JSONObject.NULL)
                .put("session_type", sessionType)

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.POST,
                resources.getString(R.string.api_url) + "register",
                null,
                { response ->
                    tryToLoginWithGoogle(userPassword, userName, userLastName, userEmail)
                },
                { error ->
                    hideLoading()

                    try {
                        Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                    } catch (err: Exception) {
                        Utilities.showRequestError(customAlertDialog, error.toString())
                    }
                }
            ) {
                override fun getBodyContentType(): String {
                    return "application/x-www-form-urlencoded"
                }

                override fun getBody(): ByteArray {
                    return "json=${URLEncoder.encode(data.toString(), "utf-8")}".toByteArray()
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
                    userToken,
                    null
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
                    userToken,
                    null
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