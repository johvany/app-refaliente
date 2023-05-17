package com.di.refaliente

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityBusinessImagesBinding
import com.di.refaliente.databinding.DialogUploadImgQuestionBinding
import com.di.refaliente.databinding.LoadingDialogBinding
import com.di.refaliente.databinding.MyDialogBinding
import com.di.refaliente.shared.ConnectionHelper
import com.di.refaliente.shared.CustomAlertDialog
import com.di.refaliente.shared.SessionHelper
import com.di.refaliente.shared.Utilities
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class BusinessImagesActivity : AppCompatActivity() {
    companion object {
        private const val BUSINESS_IMG_1 = 1
        private const val BUSINESS_IMG_2 = 2
        private const val BUSINESS_IMG_3 = 3
        private const val BUSINESS_IMG_4 = 4
    }

    private lateinit var binding: ActivityBusinessImagesBinding
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private var selectedBusinessImgNumber = 0
    private lateinit var dialogUploadBusinessImgOptions: AlertDialog
    private lateinit var dialogUploadBusinessImgView: DialogUploadImgQuestionBinding
    private lateinit var myMessageDialog: AlertDialog
    private lateinit var myMessageDialogView: MyDialogBinding
    private lateinit var customAlertDialog: CustomAlertDialog
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private var auxFilePath = ""

    @Suppress("ControlFlowWithEmptyBody")
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Photo picker launcher
        // Registers a photo picker activity launcher in single-select mode.
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                uploadBusinessImgFromGalery(uri)
            }
        }

        // Upload img options dialog
        initDialogUploadBusinessImgOptions()

        // Dialogs to show messages
        initializeDialog()
        customAlertDialog = CustomAlertDialog(this)

        // Initialize take photo launcher
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                uploadBusinessImgFromCamera()
            }
        }

        // Back arrow (in layout)
        binding.backArrow.setOnClickListener {
            finish()
        }

        // ------------------
        // Upload img buttons
        // ------------------

        // Upload img 1 button (in layout)
        binding.uploadImg1.setOnClickListener {
            selectedBusinessImgNumber = BUSINESS_IMG_1
            dialogUploadBusinessImgOptions.show()
        }

        // Upload img 2 button (in layout)
        binding.uploadImg2.setOnClickListener {
            selectedBusinessImgNumber = BUSINESS_IMG_2
            dialogUploadBusinessImgOptions.show()
        }

        // Upload img 3 button (in layout)
        binding.uploadImg3.setOnClickListener {
            selectedBusinessImgNumber = BUSINESS_IMG_3
            dialogUploadBusinessImgOptions.show()
        }

        // Upload img 4 button (in layout)
        binding.uploadImg4.setOnClickListener {
            selectedBusinessImgNumber = BUSINESS_IMG_4
            dialogUploadBusinessImgOptions.show()
        }

        setDefaultViewConfig()
        getCurrentBusinessImages(true)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun takePhoto() {
        val folder = getExternalFilesDir("business_imgs")!!.path
        val fileName = "imagen_de_mi_negocio_$selectedBusinessImgNumber.jpg"
        auxFilePath = "$folder/$fileName"
        val file = File(auxFilePath)
        if (!File(folder).exists()) { File(folder).mkdirs() }
        val fileUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePhotoLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, fileUri))
    }

    private fun initDialogUploadBusinessImgOptions() {
        dialogUploadBusinessImgOptions = MaterialAlertDialogBuilder(this).create()
        dialogUploadBusinessImgOptions.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogUploadBusinessImgOptions.setCancelable(true)
        dialogUploadBusinessImgView = DialogUploadImgQuestionBinding.inflate(layoutInflater)

        dialogUploadBusinessImgView.galeryImg.setOnClickListener {
            dialogUploadBusinessImgOptions.dismiss()
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        dialogUploadBusinessImgView.galeryText.setOnClickListener {
            dialogUploadBusinessImgOptions.dismiss()
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        dialogUploadBusinessImgView.cameraImg.setOnClickListener {
            dialogUploadBusinessImgOptions.dismiss()
            takePhoto()
        }

        dialogUploadBusinessImgView.cameraText.setOnClickListener {
            dialogUploadBusinessImgOptions.dismiss()
            takePhoto()
        }

        dialogUploadBusinessImgOptions.setView(dialogUploadBusinessImgView.root)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeDialog() {
        myMessageDialog = MaterialAlertDialogBuilder(this).create()
        myMessageDialogView = MyDialogBinding.inflate(layoutInflater)
        myMessageDialogView.title.visibility = View.GONE
        myMessageDialogView.title.text = ""
        myMessageDialogView.message.visibility = View.VISIBLE
        myMessageDialogView.message.text = ""
        myMessageDialogView.negativeButton.visibility = View.GONE
        myMessageDialogView.negativeButton.text = ""
        myMessageDialogView.negativeButton.setOnClickListener { /* ... */ }
        myMessageDialogView.positiveButton.visibility = View.VISIBLE
        myMessageDialogView.positiveButton.text = "Aceptar"
        myMessageDialogView.positiveButton.setOnClickListener { myMessageDialog.dismiss() }
        myMessageDialog.setCancelable(false)
        myMessageDialog.setView(myMessageDialogView.root)
    }

    @SuppressLint("Range", "SetTextI18n")
    private fun uploadBusinessImgFromGalery(imageUri: Uri) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            showMessage(
                "Sin conexión",
                "Por favor asegúrate de tener una conexión a internet e intenta de nuevo.",
                R.drawable.warning_dialog
            )
        } else {
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
                    var imageName: String

                    // Get not real image name
                    var cursor = contentResolver.query(imageUri, null, null, null, null)
                    cursor!!.moveToFirst()
                    imageName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    cursor.close()

                    // Try to get real name
                    cursor = contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                        "${MediaStore.Images.Media._ID} = ${imageName.substring(0, imageName.lastIndexOf('.'))}",
                        null,
                        null
                    )

                    if (cursor != null && cursor.moveToFirst()) {
                        imageName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
                        cursor.close()
                    }

                    if (cursor != null && !cursor.isClosed) {
                        cursor.close()
                    }

                    try {
                        val queryStr = "" +
                                "key_user=" + SessionHelper.user!!.sub + "&" +
                                "image_number=" + selectedBusinessImgNumber + "&" +
                                "image_name=" + URLEncoder.encode(imageName, "UTF-8")

                        (URL(resources.getString(R.string.api_url) + "save-business-image-from-app?$queryStr").openConnection() as HttpURLConnection).also { urlConn ->
                            urlConn.requestMethod = "POST"
                            urlConn.setRequestProperty("Authorization", SessionHelper.user!!.token)

                            // Upload image file.
                            urlConn.outputStream.use { connOutput ->
                                contentResolver.openInputStream(imageUri).use { imgInputStream ->
                                    imgInputStream!!.copyTo(connOutput)
                                }
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
                            setDefaultViewConfig()
                            getCurrentBusinessImages(true)
                            loading.dismiss()

                            // Show success message to the user.
                            MaterialAlertDialogBuilder(this@BusinessImagesActivity).create().also { dialog ->
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
    }

    @SuppressLint("SetTextI18n")
    private fun uploadBusinessImgFromCamera() {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            showMessage(
                "Sin conexión",
                "Por favor asegúrate de tener una conexión a internet e intenta de nuevo.",
                R.drawable.warning_dialog
            )
        } else {
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
                        val queryStr = "" +
                                "key_user=" + SessionHelper.user!!.sub + "&" +
                                "image_number=" + selectedBusinessImgNumber + "&" +
                                "image_name=" + URLEncoder.encode(imgFile.name, "UTF-8")

                        (URL(resources.getString(R.string.api_url) + "save-business-image-from-app?$queryStr").openConnection() as HttpURLConnection).also { urlConn ->
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
                            setDefaultViewConfig()
                            getCurrentBusinessImages(true)
                            imgFile.delete() // We delete image file from device because we won't need it anymore.
                            loading.dismiss()

                            // Show success message to the user.
                            MaterialAlertDialogBuilder(this@BusinessImagesActivity).create().also { dialog ->
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
    }

    private fun setDefaultViewConfig() {
        // ----------------
        // Business image 1
        // ----------------

        binding.fileNameImg1.text = "- - -"

        // binding.uploadImg1.isEnabled = false
        // binding.uploadImg1.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        // binding.uploadImg1.setColorFilter(Color.parseColor("#333333"))

        binding.showImg1.isEnabled = false
        binding.showImg1.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.showImg1.setColorFilter(Color.parseColor("#333333"))

        binding.removeImg1.isEnabled = false
        binding.removeImg1.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.removeImg1.setColorFilter(Color.parseColor("#333333"))

        // ----------------
        // Business image 2
        // ----------------

        binding.fileNameImg2.text = "- - -"

        // binding.uploadImg2.isEnabled = false
        // binding.uploadImg2.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        // binding.uploadImg2.setColorFilter(Color.parseColor("#333333"))

        binding.showImg2.isEnabled = false
        binding.showImg2.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.showImg2.setColorFilter(Color.parseColor("#333333"))

        binding.removeImg2.isEnabled = false
        binding.removeImg2.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.removeImg2.setColorFilter(Color.parseColor("#333333"))

        // ----------------
        // Business image 3
        // ----------------

        binding.fileNameImg3.text = "- - -"

        // binding.uploadImg3.isEnabled = false
        // binding.uploadImg3.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        // binding.uploadImg3.setColorFilter(Color.parseColor("#333333"))

        binding.showImg3.isEnabled = false
        binding.showImg3.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.showImg3.setColorFilter(Color.parseColor("#333333"))

        binding.removeImg3.isEnabled = false
        binding.removeImg3.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.removeImg3.setColorFilter(Color.parseColor("#333333"))

        // ----------------
        // Business image 4
        // ----------------

        binding.fileNameImg4.text = "- - -"

        // binding.uploadImg4.isEnabled = false
        // binding.uploadImg4.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        // binding.uploadImg4.setColorFilter(Color.parseColor("#333333"))

        binding.showImg4.isEnabled = false
        binding.showImg4.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.showImg4.setColorFilter(Color.parseColor("#333333"))

        binding.removeImg4.isEnabled = false
        binding.removeImg4.setBackgroundResource(R.drawable.image_button_stroked_disabled)
        binding.removeImg4.setColorFilter(Color.parseColor("#333333"))
    }

    @Suppress("SameParameterValue")
    private fun showMessage(title: String?, message: String?, icon: Int) {
        if (title == null) {
            myMessageDialogView.title.visibility = View.GONE
        } else {
            myMessageDialogView.title.visibility = View.VISIBLE
            myMessageDialogView.title.text = title
        }

        if (message == null) {
            myMessageDialogView.message.visibility = View.GONE
        } else {
            myMessageDialogView.message.visibility = View.VISIBLE
            myMessageDialogView.message.text = message
        }

        myMessageDialogView.icon.setImageResource(icon)
        myMessageDialog.show()
    }

    private fun getCurrentBusinessImages(canRepeat: Boolean) {
        Utilities.queue!!.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "get-business-images?key_user=" + SessionHelper.user!!.sub,
            null,
            { response ->
                val images = response.getJSONArray("business_images")
                val imagesSize = images.length()
                var imageItem: JSONObject

                for (i in 0 until imagesSize) {
                    imageItem = images.getJSONObject(i)
                    val imageNumberStr = imageItem.getString("image_number")
                    val imageName = imageItem.getString("image_name")

                    when (imageNumberStr.toInt()) {
                        BUSINESS_IMG_1 -> {
                            binding.fileNameImg1.text = imageItem.getString("image_name")

                            binding.showImg1.setOnClickListener {
                                startActivity(Intent(this, BusinessImageViewerActivity::class.java)
                                    .putExtra("image_number", imageNumberStr)
                                    .putExtra("image_url", "${resources.getString(R.string.api_url_storage)}${SessionHelper.user!!.sub}/business/${imageNumberStr}_$imageName"))
                            }

                            binding.showImg1.isEnabled = true
                            binding.showImg1.setBackgroundResource(R.drawable.image_button_stroked_primary)
                            binding.showImg1.setColorFilter(Color.parseColor("#1877F2"))

                            binding.removeImg1.setOnClickListener {
                                showConfirmRemoveBusinessImg(imageNumberStr)
                            }

                            binding.removeImg1.isEnabled = true
                            binding.removeImg1.setBackgroundResource(R.drawable.image_button_stroked_error)
                            binding.removeImg1.setColorFilter(Color.parseColor("#E63E3E"))
                        }
                        BUSINESS_IMG_2 -> {
                            binding.fileNameImg2.text = imageItem.getString("image_name")

                            binding.showImg2.setOnClickListener {
                                startActivity(Intent(this, BusinessImageViewerActivity::class.java)
                                    .putExtra("image_number", imageNumberStr)
                                    .putExtra("image_url", "${resources.getString(R.string.api_url_storage)}${SessionHelper.user!!.sub}/business/${imageNumberStr}_$imageName"))
                            }

                            binding.showImg2.isEnabled = true
                            binding.showImg2.setBackgroundResource(R.drawable.image_button_stroked_primary)
                            binding.showImg2.setColorFilter(Color.parseColor("#1877F2"))

                            binding.removeImg2.setOnClickListener {
                                showConfirmRemoveBusinessImg(imageNumberStr)
                            }

                            binding.removeImg2.isEnabled = true
                            binding.removeImg2.setBackgroundResource(R.drawable.image_button_stroked_error)
                            binding.removeImg2.setColorFilter(Color.parseColor("#E63E3E"))
                        }
                        BUSINESS_IMG_3 -> {
                            binding.fileNameImg3.text = imageItem.getString("image_name")

                            binding.showImg3.setOnClickListener {
                                startActivity(Intent(this, BusinessImageViewerActivity::class.java)
                                    .putExtra("image_number", imageNumberStr)
                                    .putExtra("image_url", "${resources.getString(R.string.api_url_storage)}${SessionHelper.user!!.sub}/business/${imageNumberStr}_$imageName"))
                            }

                            binding.showImg3.isEnabled = true
                            binding.showImg3.setBackgroundResource(R.drawable.image_button_stroked_primary)
                            binding.showImg3.setColorFilter(Color.parseColor("#1877F2"))

                            binding.removeImg3.setOnClickListener {
                                showConfirmRemoveBusinessImg(imageNumberStr)
                            }

                            binding.removeImg3.isEnabled = true
                            binding.removeImg3.setBackgroundResource(R.drawable.image_button_stroked_error)
                            binding.removeImg3.setColorFilter(Color.parseColor("#E63E3E"))
                        }
                        BUSINESS_IMG_4 -> {
                            binding.fileNameImg4.text = imageItem.getString("image_name")

                            binding.showImg4.setOnClickListener {
                                startActivity(Intent(this, BusinessImageViewerActivity::class.java)
                                    .putExtra("image_number", imageNumberStr)
                                    .putExtra("image_url", "${resources.getString(R.string.api_url_storage)}${SessionHelper.user!!.sub}/business/${imageNumberStr}_$imageName"))
                            }

                            binding.showImg4.isEnabled = true
                            binding.showImg4.setBackgroundResource(R.drawable.image_button_stroked_primary)
                            binding.showImg4.setColorFilter(Color.parseColor("#1877F2"))

                            binding.removeImg4.setOnClickListener {
                                showConfirmRemoveBusinessImg(imageNumberStr)
                            }

                            binding.removeImg4.isEnabled = true
                            binding.removeImg4.setBackgroundResource(R.drawable.image_button_stroked_error)
                            binding.removeImg4.setColorFilter(Color.parseColor("#E63E3E"))
                        }
                    }
                }
            },
            { error ->
                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        getCurrentBusinessImages(false)
                    } else {
                        showMessage(
                            "Operación fallida",
                            "No se pudo realizar la operación solicitada. Por favor asegúrate de tener una conexión a internet e intenta de nuevo.",
                            R.drawable.error_dialog
                        )
                    }
                }
            }
        ) { })
    }

    @SuppressLint("SetTextI18n")
    private fun showConfirmRemoveBusinessImg(imageNumber: String) {
        MaterialAlertDialogBuilder(this).create().also { dialog ->
            dialog.setCancelable(true)

            dialog.setView(MyDialogBinding.inflate(layoutInflater).also { viewBinding ->
                viewBinding.icon.setImageResource(R.drawable.question_dialog)
                viewBinding.title.visibility = View.GONE
                viewBinding.message.text = "Se borrará la imagen $imageNumber ¿Desea continuar?"

                viewBinding.positiveButton.visibility = View.VISIBLE
                viewBinding.positiveButton.text = "Sí"

                viewBinding.positiveButton.setOnClickListener {
                    dialog.dismiss()
                    removeBusinessImg(imageNumber, true)
                }

                viewBinding.negativeButton.visibility = View.VISIBLE
                viewBinding.negativeButton.text = "No"
                viewBinding.negativeButton.setBackgroundResource(R.drawable.dialog_secondary_button)

                viewBinding.negativeButton.setOnClickListener {
                    dialog.dismiss()
                }
            }.root)
        }.show()
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun removeBusinessImg(imageNumber: String, canRepeat: Boolean) {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            showMessage(
                "Sin conexión",
                "Por favor asegúrate de tener una conexión a internet e intenta de nuevo.",
                R.drawable.warning_dialog
            )
        } else {
            val data = JSONObject()
                .put("key_user", SessionHelper.user!!.sub)
                .put("image_number", imageNumber)

            Utilities.queue?.add(object: JsonObjectRequest(
                Method.PUT,
                resources.getString(R.string.api_url) + "delete-business-image",
                data,
                { response ->
                    setDefaultViewConfig()
                    getCurrentBusinessImages(true)
                },
                { error ->
                    SessionHelper.handleRequestError(error, this, customAlertDialog) {
                        if (canRepeat) {
                            removeBusinessImg(imageNumber, false)
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