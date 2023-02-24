package com.di.refaliente.home_menu_ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.PaymentActivity
import com.di.refaliente.R
import com.di.refaliente.databinding.DialogEditSelectedProductQuantityBinding
import com.di.refaliente.databinding.FragmentShoppingCartBinding
import com.di.refaliente.databinding.RowItemDividerLineBinding
import com.di.refaliente.databinding.RowItemShoppingcartProductBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.SimpleAddressAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

class ShoppingCartFragment : Fragment() {
    private lateinit var binding: FragmentShoppingCartBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private val simpleAddressesItems = ArrayList<SimpleAddress>()
    private val numberFormatHelper = NumberFormatHelper()

    // Used to edit the quantity of each product item in the shopping cart
    private var existence = 0
    private var selectedQuantity = 1
    private var selectedQuantityAux = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentShoppingCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customAlertDialog = CustomAlertDialog(requireContext())
        binding.buyShoppingCart.isEnabled = false
        getShoppingCartProducts(true)
        binding.buyShoppingCart.setOnClickListener { buyShoppingCart() }
    }

    private fun buyShoppingCart() {
        val selectedAddress = try { binding.addresses.selectedItem as SimpleAddress } catch (err: Exception) { null }

        if (selectedAddress == null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sin direcciones")
                .setMessage(
                    HtmlCompat.fromHtml(
                    "No tienes ningún domicilio guardado. Es necesario que tengas por lo menos un domicilio para poder realizar una compra. Por favor inicia sesión en <span style=\"color: #1877F2;\">www.refaliente.com</span> y agrega una dirección. Una vez agregada podrás regresar a esta pantalla y continuar con tu compra.",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ))
                .setCancelable(false)
                .setPositiveButton("ACEPTAR", null)
                .show()

        } else {
            startActivity(
                Intent(requireContext(), PaymentActivity::class.java)
                    .putExtra("key_customer", SessionHelper.user!!.sub)
                    .putExtra("single_purchase", false)
                    .putExtra("id_selected_address", selectedAddress.idAddress))
        }
    }

    @Suppress("CatchMayIgnoreException")
    @SuppressLint("SetTextI18n")
    private fun getShoppingCartProducts(canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            binding.products.removeAllViews()
            binding.noProductsMsg.visibility = View.GONE
            binding.buyShoppingCart.isEnabled = false

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                "${resources.getString(R.string.api_url)}get-shopping-cart?key_user=${SessionHelper.user!!.sub}",
                null,
                { response ->
                    if (response.has("delivery_horary") && !response.isNull("delivery_horary")) {
                        binding.message.text = response.getString("delivery_horary")
                        binding.message.visibility = View.VISIBLE
                    }

                    // Load publications of the shopping cart

                    val publications = response.getJSONArray("publications")
                    val limit = publications.length()
                    var item: JSONObject
                    var imgName: String?

                    if (limit == 0) {
                        binding.noProductsMsg.visibility = View.VISIBLE
                    } else {
                        binding.buyShoppingCart.isEnabled = true
                    }

                    for (i in 0 until limit) {
                        if (i != 0) {
                            RowItemDividerLineBinding.inflate(
                                layoutInflater,
                                binding.products,
                                true
                            )
                        }

                        item = publications.getJSONObject(i)
                        imgName = try { JSONArray(item.getString("images")).getString(0) } catch (err: Exception) { null }

                        RowItemShoppingcartProductBinding.inflate(
                            layoutInflater,
                            binding.products,
                            true
                        ).let { viewBinding ->
                            if (imgName != null) {
                                Glide.with(this)
                                    .load("${resources.getString(R.string.api_url_storage)}${item.getString("key_seller")}/products/$imgName")
                                    .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
                                    .into(viewBinding.productImg)
                            }

                            viewBinding.productTitle.text = item.getString("title")
                            viewBinding.productAmount.text = "${item.getString("selected_quantity")} x \$${numberFormatHelper.format2Decimals(item.getString("price"))}"

                            viewBinding.removeProduct.setOnClickListener {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Quitar producto")
                                    .setMessage("¿Desea quitar este producto de su carrito de compras?")
                                    .setCancelable(true)
                                    .setNegativeButton("NO", null)
                                    .setPositiveButton("SI") { _, _ -> removeProductFromShoppingCart(true, item.getInt("key_publication"), SessionHelper.user!!.sub) }
                                    .show()
                            }

                            val aux1 = item.getInt("available_quantity")
                            val aux2 = item.getInt("selected_quantity")
                            val aux3 = item.getInt("key_publication")

                            viewBinding.editQuantity.setOnClickListener {
                                existence = aux1
                                selectedQuantity = aux2
                                selectedQuantityAux = aux2
                                showChangeSelectedProductQuantity(aux3)
                            }
                        }
                    }

                    // Load summary amounts

                    binding.subtotal.text = "\$${numberFormatHelper.format2Decimals(response.getString("subtotal"))}"
                    binding.discount.text = "\$${numberFormatHelper.format2Decimals(response.getString("discount"))}"
                    binding.iva.text = "\$${numberFormatHelper.format2Decimals(response.getString("iva"))}"
                    binding.total.text = "\$${numberFormatHelper.format2Decimals(response.getString("total"))}"

                    // Get user addresses
                    try {
                        getUserAddresses(SessionHelper.user!!.sub.toString(), true)
                    } catch (err: Exception) { }
                },
                { error ->
                    SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                        if (canRepeat) {
                            getShoppingCartProducts(false)
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

    private fun showChangeSelectedProductQuantity(keyPublication: Int) {
        MaterialAlertDialogBuilder(requireContext()).create().also { dialog ->
            dialog.setCancelable(false)
            dialog.setView(DialogEditSelectedProductQuantityBinding.inflate(layoutInflater).also { viewBinding ->
                viewBinding.quantity.text = selectedQuantityAux.toString()

                viewBinding.quantityRemove.setOnClickListener {
                    if (existence != 0) {
                        if (selectedQuantityAux != 1) {
                            selectedQuantityAux -= 1
                            viewBinding.quantity.text = selectedQuantityAux.toString()
                        }
                    }
                }

                viewBinding.quantityAdd.setOnClickListener {
                    if (existence != 0) {
                        if (selectedQuantityAux != existence) {
                            selectedQuantityAux += 1
                            viewBinding.quantity.text = selectedQuantityAux.toString()
                        }
                    }
                }

                viewBinding.save.setOnClickListener {
                    selectedQuantity = selectedQuantityAux
                    dialog.dismiss()
                    updatePublicationInShoppingCart(keyPublication, true)
                }

                viewBinding.cancel.setOnClickListener {
                    selectedQuantityAux = selectedQuantity
                    dialog.dismiss()
                }
            }.root)
        }.show()
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun updatePublicationInShoppingCart(keyPublication: Int, canRepeat: Boolean) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.PUT,
            "${resources.getString(R.string.api_url)}edit-publication-in-shopping-cart",
            JSONObject()
                .put("key_user", SessionHelper.user!!.sub)
                .put("key_publication", keyPublication)
                .put("quantity", selectedQuantity),
            { response ->
                getShoppingCartProducts(true)
            },
            { error ->
                SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                    if (canRepeat) {
                        updatePublicationInShoppingCart(keyPublication, canRepeat)
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

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun removeProductFromShoppingCart(canRepeat: Boolean, keyPublication: Int, keyUser: Int) {
        if (ConnectionHelper.getConnectionType(requireContext()) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.PUT,
                "${resources.getString(R.string.api_url)}remove-publication-from-shopping-cart",
                JSONObject()
                    .put("key_publication", keyPublication)
                    .put("key_user", keyUser),
                { response ->
                    Toast.makeText(requireContext(), "Se quitó el producto", Toast.LENGTH_LONG).show()
                    getShoppingCartProducts(true)
                },
                { error ->
                    SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                        if (canRepeat) {
                            removeProductFromShoppingCart(false, keyPublication, keyUser)
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

    private fun getUserAddresses(idUser: String, canRepeat: Boolean) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "get-addresses-by-user?key_user=" + idUser,
            null,
            { response ->
                loadUserAddresses(response)
            },
            { error ->
                SessionHelper.handleRequestError(error, requireContext(), customAlertDialog) {
                    if (canRepeat) {
                        getUserAddresses(idUser, false)
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

    private fun loadUserAddresses(data: JSONObject) {
        simpleAddressesItems.clear()

        data.getJSONArray("addresses").let { addresses ->
            val limit = addresses.length()
            var item: JSONObject
            var itemPostalCode: JSONObject

            for (i in 0 until limit) {
                item = addresses.getJSONObject(i)
                itemPostalCode = item.getJSONObject("zipcode_data")

                simpleAddressesItems.add(SimpleAddress(
                    item.getInt("id_address"),
                    getAddressName(item, itemPostalCode)
                ))
            }

            binding.addresses.adapter = SimpleAddressAdapter(simpleAddressesItems, layoutInflater)
        }
    }

    private fun getAddressName(item: JSONObject, itemPostalCode: JSONObject): String {
        return "${item.getString("street")} " +
                "# ${item.getString("outside_number")}, " +
                "CP ${itemPostalCode.getString("zipcode")}, " +
                "${itemPostalCode.getString("municipality_name")} " +
                itemPostalCode.getString("entity_name")
    }
}