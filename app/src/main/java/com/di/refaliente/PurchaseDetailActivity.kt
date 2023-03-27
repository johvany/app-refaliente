package com.di.refaliente

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityPurchaseDetailBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.home_menu_ui.PurchasesFragment
import com.di.refaliente.shared.CustomAlertDialog
import com.di.refaliente.shared.PurchaseDetail
import com.di.refaliente.shared.SessionHelper
import com.di.refaliente.shared.Utilities
import com.di.refaliente.view_adapters.PurchasesDetailsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetTextI18n")
class PurchaseDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPurchaseDetailBinding
    private val purchaseDetail = ArrayList<PurchaseDetail>()
    private lateinit var customAlertDialog: CustomAlertDialog

    private lateinit var dialog: AlertDialog
    private lateinit var dialogBinding: MyDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener { finish() }
        binding.comeback.setOnClickListener { finish() }
        customAlertDialog = CustomAlertDialog(this)
        binding.purchasesDetails.layoutManager = LinearLayoutManager(this)

        binding.purchasesDetails.adapter = PurchasesDetailsAdapter(purchaseDetail, this) { idSaleDetail, comment, qualification ->
            if (comment == "") {
                showMessage(null, "El campo \"Comentario\" es obligatorio", R.drawable.warning_dialog)
            } else if (qualification == 0) {
                showMessage(null, "El campo \"Calificación\" es obligatorio", R.drawable.warning_dialog)
            } else {
                saveProductComment(idSaleDetail, comment, qualification, true)
            }
        }

        initializeDialog()
        fillPurchaseDetail(intent.getStringExtra("purchase_detail")!!)
        binding.title.text = "Compra ID: ${intent.getStringExtra("id_purchase_formatted")}"
    }

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
        if (title != null) {
            dialogBinding.title.visibility = View.VISIBLE
            dialogBinding.title.text = title
        } else {
            dialogBinding.title.visibility = View.GONE
        }

        if (message != null) {
            dialogBinding.message.visibility = View.VISIBLE
            dialogBinding.message.text = message
        } else {
            dialogBinding.message.visibility = View.GONE
        }

        dialogBinding.icon.setImageResource(icon)
        dialog.show()
    }

    private fun fillPurchaseDetail(purchaseDetailJsonArrayStr: String) {
        val jsonArray = JSONArray(purchaseDetailJsonArrayStr)
        val limit = jsonArray.length()
        var item: JSONObject
        var commentItem: JSONObject

        for (i in 0 until limit) {
            item = jsonArray.getJSONObject(i)
            commentItem = item.getJSONObject("comment_data")

            purchaseDetail.add(PurchaseDetail(
                item.getInt("id_sale_detail"),
                item.getString("product_name"),
                if (item.isNull("images")) { null } else { item.getString("images") },
                item.getString("seller_name"),
                item.getInt("key_seller"),
                item.getString("product_price"),
                item.getString("quantity"),
                item.getString("subtotal"),
                item.getString("iva"),
                item.getString("discount"),
                item.getString("total"),
                item.getBoolean("ready_to_comment"),
                false,
                0,
                if (commentItem.isNull("id_comment")) { null } else { commentItem.getInt("id_comment") },
                if (commentItem.isNull("comment")) { null } else { commentItem.getString("comment") },
                if (commentItem.isNull("qualification")) { null } else { commentItem.getInt("qualification") },
                if (commentItem.isNull("created_at")) { null } else { commentItem.getString("created_at") }
            ))
        }

        binding.purchasesDetails.adapter?.notifyItemRangeInserted(0, limit)
    }

    private fun saveProductComment(idSaleDetail: Int, comment: String, qualification: Int, canRepeat: Boolean) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.POST,
            resources.getString(R.string.api_url) + "products/comments/add",
            JSONObject()
                .put("id_purchase_detail", idSaleDetail)
                .put("comment", comment)
                .put("qualification", qualification),
            { response ->
                for (i in 0 until purchaseDetail.size step 1) {
                    if (purchaseDetail[i].idSaleDetail == idSaleDetail) {
                        purchaseDetail[i].idComment = response.getInt("id_comment")
                        purchaseDetail[i].comment = response.getString("comment")
                        purchaseDetail[i].commentQualification = response.getInt("qualification")
                        purchaseDetail[i].commentDate = response.getString("created_at")
                        binding.purchasesDetails.adapter?.notifyItemChanged(i)
                        break
                    }
                }

                showMessage(
                    "Comentario guardado",
                    null,
                    R.drawable.success_dialog
                )

                setResult(PurchasesFragment.NEW_PRODUCT_COMMENT)
            },
            { error ->
                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        saveProductComment(idSaleDetail, comment, qualification, false)
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