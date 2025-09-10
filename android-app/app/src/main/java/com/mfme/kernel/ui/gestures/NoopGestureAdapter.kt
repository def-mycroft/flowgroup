package com.mfme.kernel.ui.gestures

import com.mfme.kernel.ui.KernelViewModel

/** Simple adapter that records gesture receipts through the ViewModel. */
class NoopGestureAdapter(
    private val viewModel: KernelViewModel,
    private val windowMs: Long = 300,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) : GestureAdapter {
    private var lastTap = 0L
    override fun on(intent: GestureIntent) {
        if (intent == GestureIntent.Mark) {
            val now = nowMs()
            if (now - lastTap < windowMs) return
            lastTap = now
        }
        viewModel.logGesture(intent)
    }
}
