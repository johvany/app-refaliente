package com.di.refaliente

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityRegisterBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.shared.ConnectionHelper
import com.di.refaliente.shared.ConstantValues
import com.di.refaliente.shared.UserTypeItem
import com.di.refaliente.shared.Utilities
import com.di.refaliente.view_adapters.UserTypeAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val userTypes = ArrayList<UserTypeItem>()
    private lateinit var dialogBinding: MyDialogBinding
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDialog()

        // Back arrow button
        binding.backArrow.setOnClickListener { finish() }

        initFieldTitles()

        // User types
        getUserTypes()

        // Register button
        binding.register.setOnClickListener {
            binding.register.isClickable = false
            registerUser()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initDialog() {
        dialogBinding = MyDialogBinding.inflate(layoutInflater)
        dialogBinding.icon.setImageResource(R.drawable.warning_dialog)
        dialogBinding.title.visibility = View.VISIBLE
        dialogBinding.title.text = ""
        dialogBinding.message.visibility = View.VISIBLE
        dialogBinding.message.text = ""
        dialogBinding.negativeButton.visibility = View.GONE
        dialogBinding.negativeButton.text = ""
        dialogBinding.negativeButton.setOnClickListener { dialog.dismiss() }
        dialogBinding.positiveButton.visibility = View.VISIBLE
        dialogBinding.positiveButton.text = "Aceptar"
        dialogBinding.positiveButton.setOnClickListener { dialog.dismiss() }
        dialog = MaterialAlertDialogBuilder(this).create()
        dialog.setCancelable(false)
        dialog.setView(dialogBinding.root)
    }

    private fun showErrorMessage(title: String, message: String) {
        dialogBinding.title.text = title
        dialogBinding.message.text = message
        dialog.show()
    }

    // Here we set the title of all fields and set a '*' character at the of the name, of red color.
    private fun initFieldTitles() {
        binding.nameFieldTitle.text = HtmlCompat.fromHtml("Nombre <span style=\"color: #DA0000\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.lastNameFieldTitle.text = HtmlCompat.fromHtml("Apellidos <span style=\"color: #DA0000\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.userTypeFieldTitle.text = HtmlCompat.fromHtml("Tipo de usuario <span style=\"color: #DA0000\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.emailFieldTitle.text = HtmlCompat.fromHtml("Correo <span style=\"color: #DA0000\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.passwordFieldTitle.text = HtmlCompat.fromHtml("Contraseña <span style=\"color: #DA0000\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.passwordConfirmFieldTitle.text = HtmlCompat.fromHtml("Confirmar contraseña <span style=\"color: #DA0000\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    // This validates all fields and if someone is wrong their appearance will be changed and an error
    // message will be showed. Return true only if all fields are ok.
    @Suppress("RegExpSimplifiable")
    private fun allFieldsOk(): Boolean {
        var allOk = true

        // Nombre
        if (binding.name.text.isNullOrBlank()) {
            allOk = false
            binding.nameFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Nombre</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.nameFieldContainer.isErrorEnabled = true
        } else {
            binding.nameFieldContainer.isErrorEnabled = false
        }

        // Apellidos
        if (binding.lastName.text.isNullOrBlank()) {
            allOk = false
            binding.lastNameFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Apellidos</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.lastNameFieldContainer.isErrorEnabled = true
        } else {
            binding.lastNameFieldContainer.isErrorEnabled = false
        }

        // Tipo de usuario
        val userTypeItem = try { binding.userTypes.selectedItem as UserTypeItem } catch (err: Exception) { null }
        if (userTypeItem == null || userTypeItem.idUserType == 0) {
            allOk = false
            binding.userTypesFieldContainer.setBackgroundResource(R.drawable.dark_field_background_error)
            binding.userTypeError.text = HtmlCompat.fromHtml("El campo <strong>Tipo de usuario</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.userTypeError.visibility = View.VISIBLE
        } else {
            binding.userTypesFieldContainer.setBackgroundResource(R.drawable.dark_field_background)
            binding.userTypeError.visibility = View.GONE
        }

        // Nombre de la empresa
        if (userTypeItem != null && userTypeItem.idUserType == 2) { // 2 = Empresa
            binding.enterpriseNameFieldTitle.visibility = View.VISIBLE
            binding.enterpriseNameFieldContainer.visibility = View.VISIBLE
            if (binding.enterpriseName.text.isNullOrBlank()) {
                allOk = false
                binding.enterpriseNameFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Nombre de la empresa</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.enterpriseNameFieldContainer.isErrorEnabled = true
            } else {
                binding.enterpriseNameFieldContainer.isErrorEnabled = false
            }
        } else {
            binding.enterpriseNameFieldTitle.visibility = View.GONE
            binding.enterpriseNameFieldContainer.visibility = View.GONE
        }

        // Correo
        if (binding.email.text.isNullOrBlank()) {
            allOk = false
            binding.emailFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Correo</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.emailFieldContainer.isErrorEnabled = true
        } else if (!Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}\$", Pattern.UNICODE_CASE).matcher(binding.email.text.toString()).matches()) {
            allOk = false
            binding.emailFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Correo</strong> no es válido", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.emailFieldContainer.isErrorEnabled = true
        } else {
            binding.emailFieldContainer.isErrorEnabled = false
        }

        // Contraseña
        if (binding.password.text.isNullOrBlank()) {
            allOk = false
            binding.passwordFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Contraseña</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.passwordFieldContainer.isErrorEnabled = true
        } else if (!Pattern.compile("^(?=.*[0-9])(?=.*[!@#\$%^&*])[a-zA-Z0-9!@#\$%^&*]{8,30}\$", Pattern.UNICODE_CASE).matcher(binding.password.text.toString()).matches()) {
            allOk = false
            binding.passwordFieldContainer.error = HtmlCompat.fromHtml("La contraseña debe tener al menos 8 caracteres, 1 número, 1 letra y 1 carácter especial (!@#\$%)", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.passwordFieldContainer.isErrorEnabled = true
        } else {
            binding.passwordFieldContainer.isErrorEnabled = false
        }

        // Confirmar contraseña
        if (binding.passwordConfirm.text.isNullOrBlank()) {
            allOk = false
            binding.passwordConfirmFieldContainer.error = HtmlCompat.fromHtml("El campo <strong>Confirmar contraseña</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.passwordConfirmFieldContainer.isErrorEnabled = true
        } else if (!binding.password.text.toString().equals(binding.passwordConfirm.text.toString(), false) && allOk) {
            allOk = false
            binding.passwordConfirmFieldContainer.isErrorEnabled = false
            showErrorMessage("Las contraseñas no coinciden", "Verifique las contraseñas")
        } else {
            binding.passwordConfirmFieldContainer.isErrorEnabled = false
        }

        // Términos y condiciones
        if (!binding.termsAndConditionsCheck.isChecked && allOk) {
            allOk = false
            showErrorMessage("Debe aceptar los términos y condiciones", "Por favor acepte los términos y condiciones para poder finalizar su registro")
        }

        return allOk
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun getUserTypes() {
        if (ConnectionHelper.getConnectionType(this) != ConnectionHelper.NONE) {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "users/types",
                null,
                { response ->
                    val data = response.getJSONArray("user_types")
                    val dataSize = data.length()
                    var item: JSONObject
                    userTypes.add(UserTypeItem(0, "- - Seleccione el tipo de usuario - -")) // Add default item
                    for (i in 0 until dataSize step 1) {
                        item = data.getJSONObject(i)
                        userTypes.add(UserTypeItem(
                            item.getInt("id_user_type"),
                            item.getString("name"),
                        ))
                    }
                    binding.userTypes.adapter = UserTypeAdapter(userTypes, layoutInflater)
                    binding.userTypes.onItemSelectedListener = object: OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val userTypeItem = try { binding.userTypes.selectedItem as UserTypeItem } catch (err: Exception) { null }
                            if (userTypeItem != null && userTypeItem.idUserType == 2) { // 2 = Empresa
                                binding.enterpriseNameFieldTitle.visibility = View.VISIBLE
                                binding.enterpriseNameFieldContainer.visibility = View.VISIBLE
                            } else {
                                binding.enterpriseNameFieldTitle.visibility = View.GONE
                                binding.enterpriseNameFieldContainer.visibility = View.GONE
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            binding.enterpriseNameFieldTitle.visibility = View.GONE
                            binding.enterpriseNameFieldContainer.visibility = View.GONE
                        }
                    }
                },
                { error ->
                    // Handle response error here if you need it.
                }
            ) { })
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER", "SetTextI18n")
    private fun registerUser() {
        if (allFieldsOk() && ConnectionHelper.getConnectionType(this) != ConnectionHelper.NONE) {
            // Here we consume an endpoint to register the new user.

            // Collect  data
            val userTypeItem = binding.userTypes.selectedItem as UserTypeItem
            val data = JSONObject()
            data.put("name", binding.name.text.toString())
            data.put("surname", binding.lastName.text.toString())
            data.put("email", binding.email.text.toString())
            data.put("password", binding.password.text.toString())
            data.put("key_type_user", userTypeItem.idUserType)
            data.put("key_business_type", 1)
            data.put("session_type", "e")
            if (userTypeItem.idUserType == 2) { // 2 = Empresa
                data.put("enterprise_name", binding.enterpriseName.text.toString())
            }

            // Send request
            Utilities.queue!!.add(object: JsonObjectRequest(
                Method.POST,
                resources.getString(R.string.api_url) + "register",
                JSONObject().put("json", data.toString()),
                { response->
                    MaterialAlertDialogBuilder(this).create().also { dialog ->
                        dialog.setCancelable(false)
                        dialog.setView(MyDialogBinding.inflate(layoutInflater).also { view ->
                            view.icon.setImageResource(R.drawable.success_dialog)
                            view.title.visibility = View.VISIBLE
                            view.title.text = "Usuario registrado correctamente"
                            view.message.visibility = View.VISIBLE
                            view.message.text = response.getString("message")
                            view.negativeButton.visibility = View.GONE
                            view.negativeButton.text = ""
                            view.negativeButton.setOnClickListener { /* ... */ }
                            view.positiveButton.visibility = View.VISIBLE
                            view.positiveButton.text = "Aceptar"
                            view.positiveButton.setOnClickListener { finish() }
                        }.root)
                    }.show()
                },
                { error ->
                    try {
                        val jsonErr = JSONObject(error.networkResponse.data.decodeToString())
                        if (jsonErr.has("error") && jsonErr.getJSONObject("error").has("email")) {
                            showErrorMessage("Correo no válido", "El correo no es válido o alguien más ya lo está usando")
                        }
                    } catch (err: Exception) {
                        showErrorMessage("Ups!", "No se pudo crear la cuenta, por favor intente de nuevo y si el problema continúa contacte a soporte")
                    }
                    binding.register.isClickable = true
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
        } else if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            // Show error message here about no internet connection.
            showErrorMessage("Sin internet", "Por favor conecte su dispositivo a internet e intente de nuevo")
            binding.register.isClickable = true
        } else {
            binding.register.isClickable = true
        }
    }
}