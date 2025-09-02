package app.zero.inlet.share

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

@Deprecated("Use ShareActivity for text/plain shares")
class ShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Legacy receiver for non-text types; no-op placeholder.
    }
}
