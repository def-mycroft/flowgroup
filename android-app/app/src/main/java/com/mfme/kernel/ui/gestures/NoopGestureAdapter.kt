package com.mfme.kernel.ui.gestures

import com.mfme.kernel.ui.KernelViewModel

/** Simple adapter that records gesture receipts through the ViewModel. */
class NoopGestureAdapter(
    private val viewModel: KernelViewModel,
    private val windowMs: Long = 300,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val sink: ((GestureIntent) -> Unit)? = null,
) : GestureAdapter {
    private var lastTap = 0L
    private var hasTapped = false
    override fun on(intent: GestureIntent) {
        if (intent == GestureIntent.Mark) {
            val now = nowMs()
            if (hasTapped && now - lastTap < windowMs) return
            lastTap = now
            hasTapped = true
        }
        // Allow tests to inject a sink for deterministic counting without coroutines.
        sink?.invoke(intent) ?: viewModel.logGesture(intent)
    }
}
