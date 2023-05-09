package com.di.refaliente

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.databinding.ActivityBusinessImageViewerBinding

class BusinessImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBusinessImageViewerBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back arrow (in layout)
        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.title.text = "Imagen de mi negocio #" + intent.extras!!.getString("image_number")

        Glide.with(this)
            .load(intent.extras!!.getString("image_url"))
            .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
            .into(binding.myZoomageView)
    }
}