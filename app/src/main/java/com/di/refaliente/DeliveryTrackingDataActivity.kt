package com.di.refaliente

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityDeliveryTrackingDataBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.databinding.RowItemDeliveryTrackingItemBinding
import com.di.refaliente.shared.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

class DeliveryTrackingDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeliveryTrackingDataBinding
    private lateinit var dialog: AlertDialog
    private lateinit var dialogBinding: MyDialogBinding
    private lateinit var idPurchase: String
    private lateinit var customAlertDialog: CustomAlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryTrackingDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeDialog()
        idPurchase = intent.extras!!.getString("id_purchase")!!
        customAlertDialog = CustomAlertDialog(this)

        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.refresh.setOnRefreshListener {
            getDeliveryTrackingInitialData(true)
        }

        binding.deliveryTrackingDataContainer.visibility = View.INVISIBLE
        getDeliveryTrackingInitialData(true)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeDialog() {
        dialog = MaterialAlertDialogBuilder(this).create()
        dialogBinding = MyDialogBinding.inflate(layoutInflater)
        dialogBinding.title.visibility = View.GONE
        dialogBinding.title.text = ""
        dialogBinding.message.visibility = View.VISIBLE
        dialogBinding.message.text = ""
        dialogBinding.negativeButton.visibility = View.GONE
        dialogBinding.negativeButton.text = ""
        dialogBinding.negativeButton.setOnClickListener { /* ... */ }
        dialogBinding.positiveButton.visibility = View.VISIBLE
        dialogBinding.positiveButton.text = "Aceptar"
        dialogBinding.positiveButton.setOnClickListener { dialog.dismiss() }
        dialog.setCancelable(false)
        dialog.setView(dialogBinding.root)
    }

    private fun showMessage(title: String?, message: String?, icon: Int) {
        if (title == null) {
            dialogBinding.title.visibility = View.GONE
        } else {
            dialogBinding.title.visibility = View.VISIBLE
            dialogBinding.title.text = title
        }

        if (message == null) {
            dialogBinding.message.visibility = View.GONE
        } else {
            dialogBinding.message.visibility = View.VISIBLE
            dialogBinding.message.text = message
        }

        dialogBinding.icon.setImageResource(icon)
        dialog.show()
    }

    private fun getDeliveryTrackingInitialData(canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            binding.refresh.isRefreshing = false

            showMessage(
                "Sin conexión",
                "Por favor asegúrate de tener una conexión a internet e intenta de nuevo.",
                R.drawable.warning_dialog
            )
        } else {
            Utilities.queue?.add(object: JsonObjectRequest(
                Method.GET,
                resources.getString(R.string.api_url) + "get-purchase-delivery-trackings-by-id?" +
                        "key_user=" + SessionHelper.user!!.sub.toString() + "&" +
                        "id_purchase=" + idPurchase,
                null,
                { response ->
                    // Step 1/3
                    // Parse all received delivery trackings items and store them in 2 arrays.

                    val deliveryTrackingsAll = response.getJSONArray("delivery_trackings")
                    val deliveryTrackingsAllSize = deliveryTrackingsAll.length()
                    var deliveryTrackingsItems: JSONArray
                    var deliveryTrackingsItemsSize: Int
                    var deliveryTrackingSingleItem: JSONObject
                    val deliveryTrackingsAllParsed = ArrayList<ArrayList<DeliveryTrackingItem>>()
                    val deliveryTrackingsItemsParsed = ArrayList<DeliveryTrackingItem>()

                    for (i in 0 until deliveryTrackingsAllSize) {
                        deliveryTrackingsItems = deliveryTrackingsAll.getJSONArray(i)
                        deliveryTrackingsItemsSize = deliveryTrackingsItems.length()

                        for (j in 0 until deliveryTrackingsItemsSize) {
                            deliveryTrackingSingleItem = deliveryTrackingsItems.getJSONObject(j)

                            deliveryTrackingsItemsParsed.add(DeliveryTrackingItem(
                                deliveryTrackingSingleItem.getInt("key_deliveries_tracking_status"),
                                deliveryTrackingSingleItem.getInt("in_process"),
                                deliveryTrackingSingleItem.getInt("done"),
                                deliveryTrackingSingleItem.getString("name"),
                                deliveryTrackingSingleItem.getString("date"),
                                deliveryTrackingSingleItem.getString("description"),
                                deliveryTrackingSingleItem.getString("images"),
                                deliveryTrackingSingleItem.getString("key_delivery_man")
                            ))
                        }
                    }

                    deliveryTrackingsAllParsed.add(deliveryTrackingsItemsParsed)

                    // Step 2/3
                    // Use result arrays to generate the finally array which will be used to load
                    // the delivery tracking data in the view.

                    // NOTE:
                    // The code below was parsed from the TypeScript programing language in a web
                    // frontend made with Angular framework.

                    var status: Int
                    var showStatus7: Boolean
                    var statusInprocess: Boolean
                    val deliveryTrackingFinallyItem = ArrayList<DeliveryTrackingFinallyItem>()

                    for (ii in 0 until deliveryTrackingsAllParsed.size) {
                        showStatus7 = false

                        for (iii in 0 until deliveryTrackingsAllParsed[ii].size) {
                            if (deliveryTrackingsAllParsed[ii][iii].key_deliveries_tracking_status == 7) {
                                if (deliveryTrackingsAllParsed[ii][iii].done == 1) {
                                    showStatus7 = true
                                }

                                break
                            }
                        }

                        for (iii in 0 until deliveryTrackingsAllParsed[ii].size) {
                            status = deliveryTrackingsAllParsed[ii][iii].key_deliveries_tracking_status
                            statusInprocess = false

                            if (deliveryTrackingsAllParsed.size != 1) {
                                if (status == 3 && ii != 0) {
                                    continue
                                }
                            }

                            if (status == 6 && showStatus7) {
                                continue
                            } else if ((status == 7 || status == 8) && !showStatus7) {
                                continue
                            } else if (status == 9 && deliveryTrackingsAllParsed[ii][iii].done != 1) {
                                continue
                            }

                            val item = DeliveryTrackingFinallyItem(
                                deliveryTrackingsAllParsed[ii][iii].name,
                                deliveryTrackingsAllParsed[ii][iii].date,
                                "",
                                "",
                                deliveryTrackingsAllParsed[ii][iii].description,
                                deliveryTrackingsAllParsed[ii][iii].images,
                                deliveryTrackingsAllParsed[ii][iii].key_delivery_man,
                                status,
                                1
                            )

                            if (status == 1) {
                                item.iconTagCssClass = "fa-clock-o"
                            } else if (status == 2) {
                                item.iconTagCssClass = "fa-list-alt"

                                if (deliveryTrackingsAllParsed[ii][iii].in_process == 1) {
                                    statusInprocess = true
                                }
                            } else if (status == 3) {
                                item.iconTagCssClass = "fa-archive"

                                if (deliveryTrackingsAllParsed[ii][iii].in_process == 1) {
                                    statusInprocess = true
                                }
                            } else if (status == 4) {
                                item.iconTagCssClass = "fa-truck"

                                if (deliveryTrackingsAllParsed[ii][iii].in_process == 1) {
                                    statusInprocess = true
                                }
                            } else if (status == 5) {
                                item.iconTagCssClass = "fa-home"

                                if (deliveryTrackingsAllParsed[ii][iii].in_process == 1) {
                                    statusInprocess = true
                                }
                            } else if (status == 6) {
                                item.iconTagCssClass = "fa-check-square-o"
                            } else if (status == 7) {
                                item.iconTagCssClass = "fa-exclamation-triangle"
                            } else if (status == 8) {
                                item.iconTagCssClass = "fa-share"
                            } else if (status == 9) {
                                item.iconTagCssClass = "fa-cube"
                            }

                            if (deliveryTrackingsAllParsed[ii][iii].done == 1 || statusInprocess) {
                                if (status == 7) {
                                    item.complementTagCssClass += "circle-warning"
                                    item.iconTagCssClass += " circle-warning"
                                    item.colorId = 3
                                } else {
                                    item.complementTagCssClass += "circle-success"
                                    item.iconTagCssClass += " circle-success"
                                    item.colorId = 2
                                }
                            }
                            deliveryTrackingFinallyItem.add(item)
                        }
                    }

                    // Step 3/3
                    // Use the finally array to load the delivery tracking data.

                    binding.refresh.isRefreshing = false
                    binding.deliveryTrackingDataContainer.visibility = View.VISIBLE
                    binding.deliveryTrackingDataContainer.removeAllViews()

                    for (i in 0 until deliveryTrackingFinallyItem.size) {
                        RowItemDeliveryTrackingItemBinding.inflate(layoutInflater, binding.deliveryTrackingDataContainer, true).let { viewBinding ->
                            if (i == 0) {
                                viewBinding.complementTop.visibility = View.INVISIBLE
                            }

                            if (i == deliveryTrackingFinallyItem.size - 1) {
                                viewBinding.complementBottom.visibility = View.INVISIBLE
                            }

                            when (deliveryTrackingFinallyItem[i].statusId) {
                                1 -> viewBinding.statusIcon.setImageResource(R.drawable.baseline_watch_later_24)
                                2 -> viewBinding.statusIcon.setImageResource(R.drawable.outline_view_list_24)
                                3 -> viewBinding.statusIcon.setImageResource(R.drawable.baseline_inventory_2_24)
                                4 -> viewBinding.statusIcon.setImageResource(R.drawable.baseline_local_shipping_24)
                                5 -> viewBinding.statusIcon.setImageResource(R.drawable.baseline_home_24)
                                6 -> viewBinding.statusIcon.setImageResource(R.drawable.baseline_check_box_24)
                                7 -> viewBinding.statusIcon.setImageResource(R.drawable.round_report_problem_24)
                                8 -> viewBinding.statusIcon.setImageResource(R.drawable.round_reply_24)
                                9 -> viewBinding.statusIcon.setImageResource(R.drawable.deployed_code_48)
                            }

                            // colorId 1 = Normal
                            // colorId 2 = Success
                            // colorId 3 = Warning

                            if (deliveryTrackingFinallyItem[i].colorId == 2 || deliveryTrackingFinallyItem[i].colorId == 3) {
                                viewBinding.complementTop.setBackgroundResource(R.drawable.delivery_tracking_item_complement_success_bg)
                                viewBinding.complementBottom.setBackgroundResource(R.drawable.delivery_tracking_item_complement_success_bg)
                                viewBinding.statusIcon.setBackgroundResource(R.drawable.delivery_tracking_item_success_bg)
                            }

                            viewBinding.title.text = deliveryTrackingFinallyItem[i].name
                            viewBinding.message.text = deliveryTrackingFinallyItem[i].date
                        }
                    }
                },
                { error->
                    SessionHelper.handleRequestError(error, this, customAlertDialog) {
                        if (canRepeat) {
                            getDeliveryTrackingInitialData(false)
                        } else {
                            showMessage(
                                "Operación fallida",
                                "No se pudo realizar la operación solicitada. Por favor asegúrate de tener una conexión a internet e intenta de nuevo.",
                                R.drawable.error_dialog
                            )
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