package com.mfme.kernel.ui.gestures

/** High level gestures the UI cares about. */
sealed class GestureIntent {
    object Mark : GestureIntent()
    object MemoStart : GestureIntent()
    object MemoStop : GestureIntent()
    data class Navigate(val panelId: String) : GestureIntent()
}
