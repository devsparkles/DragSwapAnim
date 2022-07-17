package com.devsparkle.experiment.dragtoswap.extensions

import android.content.ClipData
import android.graphics.Point
import android.os.Build
import android.view.View

fun View.startDragAndDropCompat(x:Int,y: Int, data: ClipData) {
    val dragShadowBuilder = View.DragShadowBuilder(this)
    if (Build.VERSION.SDK_INT >= 24)
        startDragAndDrop(data, dragShadowBuilder, null, 0)
    else
        startDrag(data, dragShadowBuilder, null, 0)
}