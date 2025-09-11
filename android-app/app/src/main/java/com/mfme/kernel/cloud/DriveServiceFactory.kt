package com.mfme.kernel.cloud

import android.content.Context
import app.zero.core.cloud.DriveAdapter

/**
 * Provides a DriveAdapter bound to the current signed-in account/scopes if available.
 * Falls back to null when no account, allowing callers to handle auth flow prompts.
 */
object DriveServiceFactory {
    @Volatile private var cached: DriveAdapter? = null
    @Volatile private var tokenOverride: TokenProvider? = null

    fun getAdapter(context: Context): DriveAdapter? {
        // If a Google account with Drive scope is available, provide real adapter; else null.
        // We avoid holding onto access tokens directly; adapter refreshes tokens as needed.
        return cached ?: synchronized(this) {
            val provider = tokenOverride ?: GoogleSignInTokenProvider(context)
            if (provider.hasAccount()) {
                GoogleDriveAdapter(context.applicationContext, provider).also { cached = it }
            } else null
        }
    }

    fun invalidate() { cached = null }

    /** Test-only: override the TokenProvider used to determine connection state. */
    fun setTokenProviderOverrideForTests(fake: TokenProvider?) {
        tokenOverride = fake
        invalidate()
    }
}
