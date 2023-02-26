package com.di.refaliente.home_menu_ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.NewAddressActivity
import com.di.refaliente.ProductBuyingPreviewActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.FragmentAddressesBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.databinding.RowItemAddressBinding
import com.di.refaliente.shared.ConnectionHelper
import com.di.refaliente.shared.CustomAlertDialog
import com.di.refaliente.shared.SessionHelper
import com.di.refaliente.shared.Utilities
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

class AddressesFragment : Fragment() {
    private lateinit var binding: FragmentAddressesBinding
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var customAlertDialog: CustomAlertDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAddressesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == ProductBuyingPreviewActivity.NEW_ADDRESS_CREATED) {
                getUserAddresses(true)
            }
        }

        binding.createAddress.setOnClickListener {
            launcher.launch(Intent(requireContext(), NewAddressActivity::class.java))
        }

        binding.addressesContainerWrapper.setOnRefreshListener {
            getUserAddresses(true)
        }

        getUserAddresses(true)
    }

    @SuppressLint("SetTextI18n")
    private fun getUserAddresses(canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            binding.addressesContainerWrapper.isRefreshing = false
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            if (!binding.addressesContainerWrapper.isRefreshing) {
                binding.addressesContainerWrapper.isRefreshing = true
            }

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "get-addresses-by-user?key_user=" + SessionHelper.user!!.sub,
                null,
                { response ->
                    binding.addressesContainer.removeAllViews()

                    val addressesJSONArray = response.getJSONArray("addresses")
                    val addressesJSONArraySize = addressesJSONArray.length()
                    var addressJSONItem: JSONObject
                    var postalCodeJSONItem: JSONObject

                    for (i in 0 until addressesJSONArraySize step 1) {
                        addressJSONItem = addressesJSONArray.getJSONObject(i)
                        postalCodeJSONItem = addressJSONItem.getJSONObject("zipcode_data")

                        RowItemAddressBinding.inflate(layoutInflater, binding.addressesContainer, true).also { viewBinding ->
                            viewBinding.street.text = addressJSONItem.getString("street") + " " + addressJSONItem.getString("outside_number")
                            viewBinding.postalCode.text = postalCodeJSONItem.getString("zipcode")
                            viewBinding.township.text = postalCodeJSONItem.getString("municipality_name") + ", " + postalCodeJSONItem.getString("entity_name")

                            val idAddress = addressJSONItem.getString("id_address")

                            if (addressJSONItem.getInt("main_address") == 1) {
                                viewBinding.setAsMain.setBackgroundResource(R.drawable.primary_button_bg)
                            } else {
                                viewBinding.setAsMain.setBackgroundResource(R.drawable.secondary_button_bg)

                                viewBinding.setAsMain.setOnClickListener {
                                    setMainAddress(idAddress, true)
                                }
                            }

                            viewBinding.deleteAddress.setOnClickListener {
                                MaterialAlertDialogBuilder(requireContext()).create().also { dialog ->
                                    dialog.setCancelable(true)

                                    dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                                        viewBinding.icon.setImageResource(R.drawable.info_dialog)
                                        viewBinding.title.visibility = View.GONE
                                        viewBinding.message.text = "¿Desea eliminar la dirección?"
                                        viewBinding.positiveButton.text = "Sí"
                                        viewBinding.negativeButton.setBackgroundResource(R.drawable.dialog_secondary_button)
                                        viewBinding.negativeButton.text = "No"

                                        viewBinding.positiveButton.setOnClickListener {
                                            dialog.dismiss()
                                            deleteAddress(idAddress, true)
                                        }

                                        viewBinding.negativeButton.setOnClickListener {
                                            dialog.dismiss()
                                        }
                                    }.root)
                                }.show()
                            }
                        }
                    }

                    binding.addressesContainerWrapper.isRefreshing = false

                    if (addressesJSONArraySize > 0) {
                        binding.noAddressesMsg1.visibility = View.INVISIBLE
                        binding.noAddressesMsg2.visibility = View.INVISIBLE
                    } else {
                        binding.noAddressesMsg1.visibility = View.VISIBLE
                        binding.noAddressesMsg2.visibility = View.VISIBLE
                    }
                },
                { error ->
                    binding.addressesContainerWrapper.isRefreshing = false

                    SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                        if (canRepeat) {
                            getUserAddresses(false)
                        } else {
                            Utilities.showRequestError(customAlertDialog, null)
                        }
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
                }
            })
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun setMainAddress(idAddress: String, canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.PUT,
                resources.getString(R.string.api_url) + "set-main-user-address",
                JSONObject()
                    .put("id_address", idAddress)
                    .put("key_user", SessionHelper.user!!.sub),
                { response ->
                    getUserAddresses(true)
                },
                { error ->
                    SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                        if (canRepeat) {
                            setMainAddress(idAddress,false)
                        } else {
                            Utilities.showRequestError(customAlertDialog, null)
                        }
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
                }
            })
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER", "SetTextI18n")
    private fun deleteAddress(idAddress: String, canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.PUT,
                resources.getString(R.string.api_url) + "delete-user-address",
                JSONObject()
                    .put("id_address", idAddress)
                    .put("key_user", SessionHelper.user!!.sub),
                { response ->
                    MaterialAlertDialogBuilder(requireContext()).create().also { dialog ->
                        dialog.setCancelable(false)

                        dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                            viewBinding.icon.setImageResource(R.drawable.success_dialog)
                            viewBinding.title.visibility = View.GONE
                            viewBinding.message.text = "Dirección eliminada"
                            viewBinding.positiveButton.text = "Aceptar"
                            viewBinding.negativeButton.visibility = View.GONE
                            viewBinding.negativeButton.text = ""

                            viewBinding.positiveButton.setOnClickListener {
                                dialog.dismiss()
                            }

                            viewBinding.negativeButton.setOnClickListener {
                                dialog.dismiss()
                            }
                        }.root)
                    }.show()

                    getUserAddresses(true)
                },
                { error ->
                    SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                        if (canRepeat) {
                            deleteAddress(idAddress,false)
                        } else {
                            Utilities.showRequestError(customAlertDialog, null)
                        }
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
                }
            })
        }
    }
}