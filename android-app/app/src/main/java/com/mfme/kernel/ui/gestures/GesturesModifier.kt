package com.mfme.kernel.ui.gestures

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Binds basic MFME gestures to the supplied adapter. */
fun Modifier.mfmeGestures(adapter: GestureAdapter): Modifier =
    pointerInput(adapter) {
        detectTapGestures(
            onTap = { adapter.on(GestureIntent.Mark) },
            onLongPress = { adapter.on(GestureIntent.MemoStart) }
        )
    }
    .pointerInput(adapter) {
        detectHorizontalDragGestures { change, dragAmount ->
            change.consume()
            val id = if (dragAmount > 0) "right" else "left"
            adapter.on(GestureIntent.Navigate(id))
        }
    }
