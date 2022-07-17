package com.devsparkle.experiment.dragtoswap

import android.annotation.SuppressLint
import android.content.ClipDescription
import android.graphics.Rect
import android.os.Bundle
import android.view.DragEvent.ACTION_DRAG_ENDED
import android.view.DragEvent.ACTION_DRAG_ENTERED
import android.view.DragEvent.ACTION_DRAG_EXITED
import android.view.DragEvent.ACTION_DRAG_LOCATION
import android.view.DragEvent.ACTION_DRAG_STARTED
import android.view.DragEvent.ACTION_DROP
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.devsparkle.experiment.dragtoswap.MainActivityCoordinator.Events.ImageDropped
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_scrolling.image1
import kotlinx.android.synthetic.main.content_scrolling.image2
import kotlinx.android.synthetic.main.content_scrolling.image3
import kotlinx.android.synthetic.main.content_scrolling.image4
import kotlinx.android.synthetic.main.content_scrolling.list


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
                imageView.tag =
                    index // Quick&dirty: stash the index of this image in the ImageView tag
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
                        setOverlayImageAndStartAnimatedSelected(list, it, R.color.colorPrimary)


//                        val cliptext = "this is the item"
//                        val item = ClipData.Item(cliptext)
//                        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
//                        val data = ClipData(cliptext, mimeTypes, item)
//
//
//                        val draggedImage = ImageView(list.context)
//                        draggedImage.visibility = View.GONE
//                        list.addView(draggedImage)
//                        Glide.with(list.context)
//                            .load(imageViewSelected.drawable)
//                            .circleCrop()
//                            .into(draggedImage)
//
//                        val (height, width) = Pair(imageViewSelected.layoutParams.height,imageViewSelected.layoutParams.width)
//                        layout.width = it.width
//                        layout.height = it.height
//                        imageViewSelected.layoutParams = layout
//                        imageViewSelected.x = it.x
//                        imageViewSelected.y = it.y
//
//
//                        Log.d("App", "x: $eventX + y: $eventY")
//                        draggedImage.startDragAndDropCompat(eventX, eventY, data)
                        true

                    }

                }
                MotionEvent.ACTION_UP -> { // If we are dragging to something valid, do the swap
                    coordinator.imageDropped(eventX, eventY)

                }
            }
            true
        }

        list.setOnDragListener(dragListener)

        viewModel.events.observe(this, Observer {
            when (it) {
                is ImageDropped -> dropImage(it.x, it.y)
            }
        })
    }

    private fun setOverlayImageAndStartAnimatedSelected(
        parent: ConstraintLayout,
        imageView: ImageView,
        @ColorRes overlayColor: Int
    ) {

        val selectedImageOverlay = parent.findViewById<View>(R.id.overlay_image)
        if (selectedImageOverlay != null) {
            parent.removeView(selectedImageOverlay)
        }

        // create overlay image
        val overlay = View(list.context)
        overlay.id = R.id.overlay_image
        overlay.visibility = View.GONE
        overlay.setBackgroundColor(
            ContextCompat.getColor(
                this,
                overlayColor
            )
        )
        parent.addView(overlay)

        // give it the same size and position as the selected image
        val layout = overlay.layoutParams
        layout.width = imageView.width
        layout.height = imageView.height
        overlay.layoutParams = layout
        overlay.x = imageView.x
        overlay.y = imageView.y

        // animate from alpha 0 to alphe 0.5 the purple color indicating to the user that the image is selected
        overlay.alpha = 0.0f
        overlay.visibility = View.VISIBLE
        overlay.animate().apply {
            interpolator = LinearInterpolator()
            duration = 500
            alpha(0.5f)
            withEndAction { alpha(0.5f) }
            start()
        }
    }

    val dragListener = View.OnDragListener { view, event ->
        when (event.action) {
            ACTION_DRAG_STARTED -> {
                event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
            }
            ACTION_DRAG_ENTERED -> {
                view.invalidate()
                true
            }
            ACTION_DRAG_LOCATION -> {
                true
            }
            ACTION_DRAG_EXITED -> {
                view.invalidate()
                true
            }
            ACTION_DROP -> {
                val item = event.clipData.getItemAt(0)
                val dragData = item.text
                Toast.makeText(this, dragData, Toast.LENGTH_LONG).show()
                val view = event.localState as View
                val owner = view.parent as ConstraintLayout
                val destination = view as LinearLayout
                destination.addView(view)
                true
            }
            ACTION_DRAG_ENDED -> {
                view.invalidate()
                true
            }
            else ->
                false

        }
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