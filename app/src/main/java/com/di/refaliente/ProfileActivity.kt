package com.di.refaliente

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.di.refaliente.databinding.ActivityProfileBinding
import com.di.refaliente.databinding.DialogChangePasswordBinding
import com.di.refaliente.databinding.LoadingDialogBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.local_database.Database
import com.di.refaliente.local_database.UsersDetailsTable
import com.di.refaliente.local_database.UsersTable
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.BusinessTypesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ProfileActivity : AppCompatActivity() {
    companion object {
        // Users types
        const val USER_VENDEDOR = 1
        const val USER_EMPRESA = 2

        // Session types
        const val SESSION_EMAIL = "e"
        const val SESSION_GOOGLE = "g"
        const val SESSION_FACEBOK = "f"

        // Activity results codes
        const val USER_DATA_UPDATED = 1
    }

    private lateinit var binding: ActivityProfileBinding
    private val businessTypesItems = ArrayList<BusinessTypeItem>()
    private lateinit var customAlertDialog: CustomAlertDialog
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private var auxFilePath = ""

    private var userDataUpdated = false
    private var userImageUpdated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        customAlertDialog = CustomAlertDialog(this)

        // Initialize take photo launcher
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                uploadProfileImg()
            }
        }

        // Initialize default values.
        setStars(0)
        showHideFieldsBySessionType(SESSION_GOOGLE)
        showHideFieldsByUserType(USER_VENDEDOR)
        binding.nameFull.text = ""
        binding.save.setOnClickListener { if (allFieldsAreOk()) { confirmSaveUserData() } }

        // Get and load user data if there is a valid internet connection.
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage2(this)
        } else {
            getBusinessTypes()
            getUserDataPart1(true)
        }

        // change password button
        binding.changePassword.setOnClickListener {
            MaterialAlertDialogBuilder(this).create().also { dialog ->
                dialog.setCancelable(true)
                dialog.setView(DialogChangePasswordBinding.inflate(layoutInflater).also { viewBinding ->
                    viewBinding.cancel.setOnClickListener { dialog.dismiss() }
                    viewBinding.changePassword.setOnClickListener {
                        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
                            Utilities.showUnconnectedMessage2(this)
                        } else if (changePasswordFieldsOk(viewBinding)) {
                            changeUserPassword(viewBinding.currentPassword.text.toString(), viewBinding.newPassword.text.toString(), dialog, true)
                        }
                    }
                }.root)
            }.show()
        }

        // set change user profile event
        binding.userImg.setOnClickListener { takePhoto() }
    }

    private fun uploadProfileImg() {
        // We create and show a loading window.
        val loading = MaterialAlertDialogBuilder(this).create().also { dialog ->
            dialog.setCancelable(false)
            dialog.setView(LoadingDialogBinding.inflate(layoutInflater).also { viewBinding ->
                viewBinding.message.text = "Subiendo imagen..."
            }.root)
        }
        loading.show()

        object: Thread() {
            override fun run() {
                var responseStr: String
                var imgUploaded = false
                val imgFile = File(auxFilePath)

                try {
                    (URL(resources.getString(R.string.api_url) + "user/profile/image/upload-from-app?file_extension=jpg").openConnection() as HttpURLConnection).also { urlConn ->
                        urlConn.requestMethod = "POST"
                        urlConn.setRequestProperty("Authorization", SessionHelper.user!!.token)

                        // Upload image file.
                        urlConn.outputStream.use { connOutput ->
                            FileInputStream(imgFile).use { imgData -> imgData.copyTo(connOutput) }
                        }

                        // Read server response after upload image file.
                        if (urlConn.responseCode == 200) {
                            urlConn.inputStream.use { connInput -> responseStr = connInput.readBytes().decodeToString() }
                            imgUploaded = true
                        } else {
                            responseStr = urlConn.errorStream.readBytes().decodeToString()
                        }
                    }
                } catch (err: Exception) {
                    responseStr = err.toString()
                }

                if (imgUploaded) {
                    Handler(Looper.getMainLooper()).post {
                        binding.userImg.setImageBitmap(BitmapFactory.decodeFile(auxFilePath))
                        imgFile.delete() // We delete image file from device because we won't need it anymore.
                        loading.dismiss()

                        // TODO: refresh user data (only the image name) in the local database ...
                        val imgName = JSONObject(responseStr).getString("image_name")

                        // Show success message to the user.
                        MaterialAlertDialogBuilder(this@ProfileActivity).create().also { dialog ->
                            dialog.setCancelable(false)
                            dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                                viewBinding.icon.setImageResource(R.drawable.success_dialog)
                                viewBinding.title.text = "Imagen actualizada"
                                viewBinding.message.visibility = View.GONE
                                viewBinding.negativeButton.visibility = View.GONE
                                viewBinding.positiveButton.text = "Aceptar"
                                viewBinding.positiveButton.setOnClickListener { dialog.dismiss() }
                            }.root)
                        }.show()
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        loading.dismiss()
                        Utilities.showUnknownError(customAlertDialog, responseStr)
                    }
                }
            }
        }.start()
    }

    private fun takePhoto() {
        val folder = getExternalFilesDir("profile_imgs")!!.path
        val fileName = "image_profile_temp.jpg"
        auxFilePath = "$folder/$fileName"
        val file = File(auxFilePath)
        if (!File(folder).exists()) { File(folder).mkdirs() }
        val fileUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePhotoLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, fileUri))
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun changeUserPassword(currentPassword: String, newPassword: String, dialog: AlertDialog, canRepeat: Boolean) {
        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.POST,
            resources.getString(R.string.api_url) + "user/password/change",
            JSONObject()
                .put("current_password", currentPassword)
                .put("new_password", newPassword),
            { response ->
                dialog.dismiss()
                val db = Database(this)

                // Update user data in the local database.
                UsersTable.find(db, 1)!!.let { foundUser ->
                    UsersTable.update(db, User(
                        1,
                        foundUser.sub,
                        foundUser.email,
                        foundUser.name,
                        foundUser.surname,
                        foundUser.roleUser,
                        newPassword, // we update only the password
                        foundUser.token,
                        null
                    ))
                }

                // Refresh user shared object.
                SessionHelper.user = UsersTable.find(db, 1)

                MaterialAlertDialogBuilder(this).create().also { dialog2 ->
                    dialog2.setCancelable(false)
                    dialog2.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                        viewBinding.icon.setImageResource(R.drawable.success_dialog)
                        viewBinding.title.text = "La constraseña se actualizo con éxito"
                        viewBinding.message.visibility = View.GONE
                        viewBinding.negativeButton.visibility = View.GONE
                        viewBinding.positiveButton.text = "Aceptar"
                        viewBinding.positiveButton.setOnClickListener { dialog2.dismiss() }
                    }.root)
                }.show()
            },
            { error ->
                try {
                    val errResp = JSONObject(error.networkResponse.data.decodeToString())
                    if (errResp.has("current_password_mismatch")) {
                        MaterialAlertDialogBuilder(this).create().also { dialog2 ->
                            dialog2.setCancelable(false)
                            dialog2.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                                viewBinding.icon.setImageResource(R.drawable.error_dialog)
                                viewBinding.title.text = "Error, no se pudo cambiar la contraseña. Su contraseña actual no es correcta."
                                viewBinding.message.visibility = View.GONE
                                viewBinding.negativeButton.visibility = View.GONE
                                viewBinding.positiveButton.text = "Aceptar"
                                viewBinding.positiveButton.setOnClickListener { dialog2.dismiss() }
                            }.root)
                        }.show()
                    } else {
                        SessionHelper.handleRequestError(error, this, customAlertDialog) {
                            if (canRepeat) {
                                changeUserPassword(currentPassword, newPassword, dialog, false)
                            } else {
                                try {
                                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                                } catch (err: Exception) {
                                    Utilities.showRequestError(customAlertDialog, error.toString())
                                }
                            }
                        }
                    }
                } catch (err2: Exception) {
                    SessionHelper.handleRequestError(error, this, customAlertDialog) {
                        if (canRepeat) {
                            changeUserPassword(currentPassword, newPassword, dialog, false)
                        } else {
                            try {
                                Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                            } catch (err: Exception) {
                                Utilities.showRequestError(customAlertDialog, error.toString())
                            }
                        }
                    }
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun changePasswordFieldsOk(viewBinding: DialogChangePasswordBinding): Boolean {
        viewBinding.currentPasswordContainer.error = null
        viewBinding.currentPasswordContainer.isErrorEnabled = false
        viewBinding.newPasswordContainer.error = null
        viewBinding.newPasswordContainer.isErrorEnabled = false
        viewBinding.newPasswordConfirmContainer.error = null
        viewBinding.newPasswordConfirmContainer.isErrorEnabled = false

        var allOk = true

        if (viewBinding.currentPassword.text.isNullOrBlank()) {
            viewBinding.currentPasswordContainer.error = HtmlCompat.fromHtml("El campo <strong>Contraseña actual</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            viewBinding.currentPasswordContainer.isErrorEnabled = true
            allOk = false
        }

        if (viewBinding.newPassword.text.isNullOrBlank()) {
            viewBinding.newPasswordContainer.error = HtmlCompat.fromHtml("El campo <strong>Nueva contraseña</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            viewBinding.newPasswordContainer.isErrorEnabled = true
            allOk = false
        } else if (viewBinding.newPassword.text.toString().length < 8) {
            viewBinding.newPasswordContainer.error = HtmlCompat.fromHtml("Su nueva contraseña debe tener al menos 8 caracteres", HtmlCompat.FROM_HTML_MODE_LEGACY)
            viewBinding.newPasswordContainer.isErrorEnabled = true
            allOk = false
        }

        if (viewBinding.newPasswordConfirm.text.isNullOrBlank()) {
            viewBinding.newPasswordConfirmContainer.error = HtmlCompat.fromHtml("El campo <strong>Confirmar contraseña</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            viewBinding.newPasswordConfirmContainer.isErrorEnabled = true
            allOk = false
        } else if (!viewBinding.newPasswordConfirm.text.toString().equals(viewBinding.newPassword.text.toString(), false)) {
            viewBinding.newPasswordContainer.error = HtmlCompat.fromHtml("Las contraseñas no coinciden", HtmlCompat.FROM_HTML_MODE_LEGACY)
            viewBinding.newPasswordContainer.isErrorEnabled = true
            viewBinding.newPasswordConfirmContainer.error = HtmlCompat.fromHtml("Las contraseñas no coinciden", HtmlCompat.FROM_HTML_MODE_LEGACY)
            viewBinding.newPasswordConfirmContainer.isErrorEnabled = true
            allOk = false
        }

        return allOk
    }

    // Set color to stars of the "Calificación" field.
    private fun setStars(stars: Int) {
        when (stars) {
            1 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            2 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            3 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            4 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
            5 -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEE000"))
            }
            else -> {
                binding.star1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star4.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
                binding.star5.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D3D3D3"))
            }
        }
    }

    private fun showHideFieldsBySessionType(sessionType: String) {
        when (sessionType) {
            SESSION_EMAIL -> { binding.changePassword.visibility = View.VISIBLE }
            SESSION_GOOGLE -> { binding.changePassword.visibility = View.GONE }
            SESSION_FACEBOK -> { binding.changePassword.visibility = View.GONE }
        }
    }

    private fun showHideFieldsByUserType(userType: Int) {
        when (userType) {
            USER_VENDEDOR -> {
                binding.giroTitle.visibility = View.GONE
                binding.giroContainer.visibility = View.GONE
                binding.rfcTitle.visibility = View.GONE
                binding.rfcContainer.visibility = View.GONE
                binding.enterpriseNameTitle.visibility = View.GONE
                binding.enterpriseNameContainer.visibility = View.GONE
                binding.socialReasonTitle.visibility = View.GONE
                binding.socialReasonContainer.visibility = View.GONE
                binding.uploadBusinessImgs.visibility = View.GONE
            }
            USER_EMPRESA -> {
                binding.giroTitle.visibility = View.VISIBLE
                binding.giroContainer.visibility = View.VISIBLE
                binding.rfcTitle.visibility = View.VISIBLE
                binding.rfcContainer.visibility = View.VISIBLE
                binding.enterpriseNameTitle.visibility = View.VISIBLE
                binding.enterpriseNameContainer.visibility = View.VISIBLE
                binding.socialReasonTitle.visibility = View.VISIBLE
                binding.socialReasonContainer.visibility = View.VISIBLE
                binding.uploadBusinessImgs.visibility = View.VISIBLE
            }
        }
    }

    private fun confirmSaveUserData() {
        MaterialAlertDialogBuilder(this).create().also { dialog ->
            dialog.setCancelable(true)
            dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                viewBinding.icon.setImageResource(R.drawable.question_dialog)
                viewBinding.title.visibility = View.VISIBLE
                viewBinding.title.text = "¿Desea guardar sus cambios?"
                viewBinding.message.visibility = View.GONE
                viewBinding.positiveButton.text = "Sí"
                viewBinding.negativeButton.text = "No"
                viewBinding.positiveButton.setOnClickListener {
                    dialog.dismiss()
                    saveUserData()
                }
                viewBinding.negativeButton.setOnClickListener { dialog.dismiss() }
            }.root)
        }.show()
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun saveUserData() {
        val data = JSONObject()
            .put("name", binding.name.text.toString())
            .put("surname", binding.surname.text.toString())
            .put("description", if (binding.aboutMe.text.isNullOrBlank()) JSONObject.NULL else binding.aboutMe.text.toString())
            .put("key_business_type", (binding.businessTypes.selectedItem as BusinessTypeItem).idBusinessType)
            .put("rfc", if (binding.rfc.text.isNullOrBlank()) JSONObject.NULL else binding.rfc.text.toString())
            .put("social_reason", if (binding.socialReason.text.isNullOrBlank()) JSONObject.NULL else binding.socialReason.text.toString())
            .put("enterprise_name", if (binding.enterpriseName.text.isNullOrBlank()) JSONObject.NULL else binding.enterpriseName.text.toString())
            .put("telephone", if (binding.telephone.text.isNullOrBlank()) JSONObject.NULL else binding.telephone.text.toString())

        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.PUT,
            resources.getString(R.string.api_url) + "user/update",
            null,
            { response ->
                binding.nameFull.text = data.getString("name") + " " + data.getString("surname")
                userDataUpdated = true
                val db = Database(this)

                // Update user data in the local database.
                UsersTable.find(db, 1)!!.let { foundUser ->
                    UsersTable.update(db, User(
                        1,
                        foundUser.sub,
                        foundUser.email,
                        data.getString("name"),
                        data.getString("surname"),
                        foundUser.roleUser,
                        foundUser.password,
                        foundUser.token,
                        null
                    ))
                }

                // Update user detail in the local database.
                UsersDetailsTable.find(db, 1)!!.let { foundUserDetail ->
                    UsersDetailsTable.update(db, UserDetail(
                        1,
                        foundUserDetail.keySub,
                        data.getString("name"),
                        data.getString("surname"),
                        foundUserDetail.email,
                        if (data.getString("description") == "null") { null } else { data.getString("description") },
                        foundUserDetail.keyTypeUser,
                        data.getInt("key_business_type"),
                        if (data.getString("telephone") == "null") { null } else { data.getString("telephone") },
                        foundUserDetail.profileImage,
                        foundUserDetail.sessionType,
                        if (data.getString("rfc") == "null") { null } else { data.getString("rfc") },
                        if (data.getString("social_reason") == "null") { null } else { data.getString("social_reason") },
                        if (data.getString("enterprise_name") == "null") { null } else { data.getString("enterprise_name") }
                    ))
                }

                // Refresh user shared object.
                SessionHelper.user = UsersTable.find(db, 1)

                MaterialAlertDialogBuilder(this).create().also { dialog ->
                    dialog.setCancelable(false)
                    dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                        viewBinding.icon.setImageResource(R.drawable.success_dialog)
                        viewBinding.title.text = "Datos actualizados con éxito"
                        viewBinding.message.visibility = View.GONE
                        viewBinding.negativeButton.visibility = View.GONE
                        viewBinding.positiveButton.text = "Aceptar"
                        viewBinding.positiveButton.setOnClickListener { dialog.dismiss() }
                    }.root)
                }.show()
            },
            { error ->
                try {
                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded"
            }

            override fun getBody(): ByteArray {
                return ("json=" + URLEncoder.encode(data.toString(), "UTF-8")).toByteArray()
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun allFieldsAreOk(): Boolean {
        // -----------------------
        // Reset all fields errors
        // -----------------------

        // Nombre
        binding.nameContainer.error = null
        binding.nameContainer.isErrorEnabled = false

        // Apellido
        binding.nameContainer.error = null
        binding.nameContainer.isErrorEnabled = false

        // ----------------------------
        // Validate all required fields
        // ----------------------------

        var allOk = true

        // Nombre
        if (binding.name.text.isNullOrBlank()) {
            binding.nameContainer.error = HtmlCompat.fromHtml("El campo <strong>Nombre</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.nameContainer.isErrorEnabled = true
            allOk = false
        }

        // Apellido
        if (binding.surname.text.isNullOrBlank()) {
            binding.surnameContainer.error = HtmlCompat.fromHtml("El campo <strong>Apellido</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.surnameContainer.isErrorEnabled = true
            allOk = false
        }

        return allOk
    }

    // Get data for "Giro" field.
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun getBusinessTypes() {
        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "get-business-types",
            null,
            { response ->
                // Load business types in the field of type select (spinner).

                val items = response.getJSONArray("business_types")
                val itemsLength = items.length()
                var item: JSONObject

                for (i in 0 until itemsLength) {
                    item = items.getJSONObject(i)
                    businessTypesItems.add(BusinessTypeItem(item.getInt("id_business_type"), item.getString("name")))
                }

                binding.businessTypes.adapter = BusinessTypesAdapter(businessTypesItems, layoutInflater)
            },
            { error ->
                // Handle request error here if you need.
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

    // Get data for most fields.
    private fun getUserDataPart1(canRepeat: Boolean) {
        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.POST,
            resources.getString(R.string.api_url) + "login",
            null,
            { response ->
                getUserDataPart2(response, true)
            },
            { error ->
                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        getUserDataPart1(false)
                    } else {
                        try {
                            Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                        } catch (err: Exception) {
                            Utilities.showRequestError(customAlertDialog, error.message)
                        }
                    }
                }
            }
        ) {
            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded"
            }

            override fun getBody(): ByteArray {
                return (
                        "json=" + URLEncoder.encode(
                            JSONObject()
                                .put("email", SessionHelper.user!!.email)
                                .put("password", SessionHelper.user!!.password)
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

    // Get data for "Publicaciones" and "Calificación" fields.
    private fun getUserDataPart2(userDataPart1: JSONObject, canRepeat: Boolean) {
        Volley.newRequestQueue(this).add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "user/profile/extra-data",
            null,
            { response ->
                loadUserData(userDataPart1, response)
            },
            { error ->
                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        getUserDataPart2(userDataPart1, false)
                    } else {
                        try {
                            Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                        } catch (err: Exception) {
                            Utilities.showRequestError(customAlertDialog, error.message)
                        }
                    }
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun loadUserData(userDataPart1: JSONObject, userDataPart2: JSONObject) {
        val details = userDataPart1.getJSONObject("details")
        val keyTypeUser = details.getInt("key_type_user")
        val name = details.getString("name")
        val surname = details.getString("surname")
        showHideFieldsBySessionType(details.getString("session_type"))
        showHideFieldsByUserType(keyTypeUser)

        // Publicaciones
        binding.publicationsTotal.text = userDataPart2.getString("publications_total")

        // Calificación
        setStars(userDataPart2.getInt("qualification_stars"))

        // Perfil
        binding.perfil.text = if (keyTypeUser == 1) "Estándar" else "Empresa"

        // Nombre (full)
        binding.nameFull.text = "$name $surname"

        // Nombre
        binding.name.setText(name)

        // Apellido
        binding.surname.setText(surname)

        // Email
        binding.email.setText(details.getString("email"))

        // Giro
        val businessTypesItemsSize = businessTypesItems.size
        val keyBusinessType = details.getInt("key_business_type")
        for (i in 0 until businessTypesItemsSize) {
            if (businessTypesItems[i].idBusinessType == keyBusinessType) {
                binding.businessTypes.setSelection(i)
                break
            }
        }

        // RFC
        binding.rfc.setText(if (details.getString("rfc") == "null") "" else details.getString("rfc"))

        // Nombre de la empresa
        binding.enterpriseName.setText(if (details.getString("enterprise_name") == "null") "" else details.getString("enterprise_name"))

        // Teléfono
        binding.telephone.setText(if (details.getString("telephone") == "null") "" else details.getString("telephone"))

        // Razón social
        binding.socialReason.setText(if (details.getString("social_reason") == "null") "" else details.getString("social_reason"))

        // Acerca de mi
        binding.aboutMe.setText(if (details.getString("description") == "null") "" else details.getString("description"))
    }

    override fun onBackPressed() {
        if (userDataUpdated || userImageUpdated) {
            setResult(USER_DATA_UPDATED, Intent()
                .putExtra("user_data_updated", userDataUpdated)
                .putExtra("user_image_updated", userImageUpdated))
        }

        super.onBackPressed()
    }
}