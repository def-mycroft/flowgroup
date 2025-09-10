package com.mfme.kernel.ui.gestures

/** Adapter receiving gesture intents. */
fun interface GestureAdapter {
    fun on(intent: GestureIntent)
}
