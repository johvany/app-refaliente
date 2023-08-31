package com.di.refaliente

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.di.refaliente.databinding.ActivityDeliveryTrackingIncidenceImgViewerBinding

class DeliveryTrackingIncidenceImgViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeliveryTrackingIncidenceImgViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryTrackingIncidenceImgViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back arrow (in layout)
        binding.backArrow.setOnClickListener {
            finish()
        }

        Glide.with(this)
            .load(intent.extras!!.getString("image_url"))
            .apply(RequestOptions.skipMemoryCacheOf(true)) // Uncomment if you want to always refresh the image
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // Uncomment if you want to always refresh the image
            .into(binding.myZoomageView)
    }
}