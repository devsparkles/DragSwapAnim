package com.devsparkle.experiment.dragtoswap

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.devsparkle.experiment.dragtoswap.MainActivityCoordinator.Events.ImageDropped
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_scrolling.*


/**
 * Place for applying view data to views, and passing actions to coordinator
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var coordinator: MainActivityCoordinator

    private val imageViews: List<ImageView> by lazy { listOf(image1, image2, image3, image4) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coordinator = MainActivityCoordinator(viewModel)
        setSupportActionBar(toolbar)
        toolbar.title = title

        viewModel.images.observe(this, Observer { images ->
            // Load all the images from the viewModel into ImageViews
            imageViews.forEachIndexed { index, imageView ->
                Glide.with(this)
                    .load(images[index].imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(imageView)
                imageView.tag = index // Quick&dirty: stash the index of this image in the ImageView tag
            }
        })

        list.setOnTouchListener { _, event ->
            val eventX = event.x.toInt()
            val eventY = event.y.toInt()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { // Hunt for what's under the drag and start dragging
                    getImageViewAt(eventX, eventY)?.let {
                        val index = it.tag as Int
                        coordinator.startedSwap(index)
                    }
                }
                MotionEvent.ACTION_UP -> { // If we are dragging to something valid, do the swap
                    coordinator.imageDropped(eventX, eventY)
                }
            }
            true
        }

        viewModel.events.observe(this, Observer {
            when (it) {
                is ImageDropped -> dropImage(it.x, it.y)
            }
        })
    }

    private fun dropImage(eventX: Int, eventY: Int) {
        val sourceImageIndex = viewModel.draggingIndex.value
        val targetImage = getImageViewAt(eventX, eventY)
        val targetImageIndex = targetImage
            ?.let { it.tag as Int }
        if (targetImageIndex != null && sourceImageIndex != null && targetImageIndex != sourceImageIndex)
            coordinator.swapImages(sourceImageIndex, targetImageIndex)
        else
            coordinator.cancelSwap()
    }

    private fun getImageViewAt(x: Int, y: Int): ImageView? {
        val hitRect = Rect()
        return imageViews.firstOrNull {
            it.getHitRect(hitRect)
            hitRect.contains(x, y)
        }
    }

}