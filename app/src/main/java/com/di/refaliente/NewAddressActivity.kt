package com.di.refaliente

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityNewAddressBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.ColoniasAdapter
import org.json.JSONObject

class NewAddressActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewAddressBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private val zipcodes = ArrayList<Zipcode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customAlertDialog = CustomAlertDialog(this)
        initAddressFields()

        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.containerCodigoPostal.setEndIconOnClickListener {
            binding.codigoPostal.text.toString().let { postalCode ->
                if (postalCode == "") {
                    binding.containerCodigoPostal.error = HtmlCompat.fromHtml("El campo <strong>Código postal</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.containerCodigoPostal.isErrorEnabled = true
                } else {
                    binding.containerCodigoPostal.error = null
                    binding.containerCodigoPostal.isErrorEnabled = false
                    verifyPostalCode(postalCode, true)
                }
            }
        }

        binding.saveNewAddress.setOnClickListener {
            resetAddressFieldsErrors()

            if (allAddressFieldsOk()) {
                saveNewAddress(true)
            }
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun saveNewAddress(canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            val zipcode = binding.colonias.selectedItem as Zipcode
            val numeroInterior = binding.numeroInterior.text.toString()
            val informacion = binding.informacionAdicional.text.toString()

            val data = JSONObject()
                .put("id_address", 0)
                .put("key_user", SessionHelper.user!!.sub)
                .put("key_zipcode", zipcode.idZipcode)
                .put("street", binding.calle.text.toString())
                .put("phone_number", binding.telefonoContacto.text.toString())
                .put("outside_number", binding.numeroExterior.text.toString())
                .put("inside_number", if (numeroInterior == "") { JSONObject.NULL } else { numeroInterior })
                .put("street_1", binding.entreCalle1.text.toString())
                .put("street_2", binding.entreCalle2.text.toString())
                .put("information", if (informacion == "") { JSONObject.NULL } else { informacion })

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.POST,
                resources.getString(R.string.api_url) + "add-new-address",
                data,
                { response ->
                    setResult(ProductBuyingPreviewActivity.NEW_ADDRESS_CREATED)
                    finish()
                    Toast.makeText(this, "Se guardó la nueva dirección", Toast.LENGTH_LONG).show()
                },
                { error ->
                    SessionHelper.handleRequestError(error, this, customAlertDialog) {
                        if (canRepeat) {
                            saveNewAddress(false)
                        } else {
                            Utilities.showRequestError(customAlertDialog, null)
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
    }

    private fun verifyPostalCode(postalCode: String, canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "get-zipcodes-data?zipcode=" + postalCode,
                null,
                { response ->
                    val data = response.getJSONArray("zipcodes")
                    var item: JSONObject
                    val limit = data.length()
                    zipcodes.clear()

                    for (i in 0 until limit) {
                        item = data.getJSONObject(i)

                        if (i == 0) {
                            zipcodes.add(Zipcode(
                                0,
                                0,
                                null,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                            ))
                        } else {
                            zipcodes.add(Zipcode(
                                item.getInt("id_zipcode"),
                                item.getInt("zipcode"),
                                item.getString("township_key").toIntOrNull(),
                                item.getString("township_name"),
                                getStrOrNull(item.getString("township_type")),
                                item.getString("township_type_code").toIntOrNull(),
                                getStrOrNull(item.getString("township_zone")),
                                getStrOrNull(item.getString("municipality_name")),
                                item.getString("municipality_key").toIntOrNull(),
                                getStrOrNull(item.getString("entity_name")),
                                item.getString("entity_key").toIntOrNull(),
                                getStrOrNull(item.getString("city_name")),
                                getStrOrNull(item.getString("city_key")),
                                item.getString("pc_administration").toIntOrNull(),
                                item.getString("pc_administration_office").toIntOrNull(),
                                getStrOrNull(item.getString("created_at")),
                                getStrOrNull(item.getString("updated_at"))
                            ))
                        }
                    }

                    binding.colonias.adapter = ColoniasAdapter(zipcodes, layoutInflater)

                    if (zipcodes.size > 0) {
                        binding.estado.setText(zipcodes[1].entityName)
                    } else {
                        binding.estado.setText("")
                    }
                },
                { error ->
                    SessionHelper.handleRequestError(error, this, customAlertDialog) {
                        if (canRepeat) {
                            verifyPostalCode(postalCode, false)
                        } else {
                            Utilities.showRequestError(customAlertDialog, null)
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
    }

    private fun getStrOrNull(value: String): String? {
        return if (value == "null") { null } else { value }
    }

    private fun initAddressFields() {
        // Set fields names. This set a red character ("*") that indicates the field is required.
        binding.containerCodigoPostal.errorIconDrawable = null
        binding.coloniaError.text = HtmlCompat.fromHtml("<span style=\"color: #DA0000;\">El campo <strong>Colonia</strong> es obligatorio</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.containerCodigoPostal.hint = HtmlCompat.fromHtml("Código postal <span style=\"color: #DA0000;\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.containerCalle.hint = HtmlCompat.fromHtml("Calle <span style=\"color: #DA0000;\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.containerTelefonoContacto.hint = HtmlCompat.fromHtml("Teléfono de contacto <span style=\"color: #DA0000;\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.containerNumeroExterior.hint = HtmlCompat.fromHtml("Número exterior <span style=\"color: #DA0000;\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.containerEntreCalle1.hint = HtmlCompat.fromHtml("Entre calle 1 <span style=\"color: #DA0000;\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.containerEntreCalle2.hint = HtmlCompat.fromHtml("Entre calle 2 <span style=\"color: #DA0000;\">*</span>", HtmlCompat.FROM_HTML_MODE_LEGACY)

        // Set fields to empty values
        binding.codigoPostal.setText("")
        binding.estado.setText("")
        binding.calle.setText("")
        binding.telefonoContacto.setText("")
        binding.numeroExterior.setText("")
        binding.numeroInterior.setText("")
        binding.entreCalle1.setText("")
        binding.entreCalle2.setText("")
        binding.informacionAdicional.setText("")
    }

    private fun allAddressFieldsOk(): Boolean {
        var allOk = true

        if (binding.codigoPostal.text.toString() == "") {
            binding.containerCodigoPostal.error = HtmlCompat.fromHtml("El campo <strong>Código postal</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.containerCodigoPostal.isErrorEnabled = true
            allOk = false
        }

        val colonia = try { binding.colonias.selectedItem as Zipcode } catch (err: Exception) { null }

        if (colonia == null || colonia.idZipcode == 0) {
            binding.containerColonias.setBackgroundResource(R.drawable.borders_error)
            binding.coloniaError.visibility = View.VISIBLE
        }

        if (binding.calle.text.toString() == "") {
            binding.containerCalle.error = HtmlCompat.fromHtml("El campo <strong>Calle</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.containerCalle.isErrorEnabled = true
            allOk = false
        }

        if (binding.telefonoContacto.text.toString() == "") {
            binding.containerTelefonoContacto.error = HtmlCompat.fromHtml("El campo <strong>Teléfono de contacto</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.containerTelefonoContacto.isErrorEnabled = true
            allOk = false
        }

        if (binding.numeroExterior.text.toString() == "") {
            binding.containerNumeroExterior.error = HtmlCompat.fromHtml("El campo <strong>Número exterior</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.containerNumeroExterior.isErrorEnabled = true
            allOk = false
        }

        if (binding.entreCalle1.text.toString() == "") {
            binding.containerEntreCalle1.error = HtmlCompat.fromHtml("El campo <strong>Entre calle 1</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.containerEntreCalle1.isErrorEnabled = true
            allOk = false
        }

        if (binding.entreCalle2.text.toString() == "") {
            binding.containerEntreCalle2.error = HtmlCompat.fromHtml("El campo <strong>Entre calle 2</strong> es obligatorio", HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.containerEntreCalle2.isErrorEnabled = true
            allOk = false
        }

        return allOk
    }

    private fun resetAddressFieldsErrors() {
        binding.containerCodigoPostal.error = null
        binding.containerCodigoPostal.isErrorEnabled = false

        binding.containerColonias.setBackgroundResource(R.drawable.borders_lightgray)
        binding.coloniaError.visibility = View.GONE

        binding.containerCalle.error = null
        binding.containerCalle.isErrorEnabled = false

        binding.containerTelefonoContacto.error = null
        binding.containerTelefonoContacto.isErrorEnabled = false

        binding.containerNumeroExterior.error = null
        binding.containerNumeroExterior.isErrorEnabled = false

        binding.containerEntreCalle1.error = null
        binding.containerEntreCalle1.isErrorEnabled = false

        binding.containerEntreCalle2.error = null
        binding.containerEntreCalle2.isErrorEnabled = false
    }
}