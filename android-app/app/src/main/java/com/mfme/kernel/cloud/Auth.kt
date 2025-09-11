package com.mfme.kernel.cloud

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException

/** Lightweight token provider using Google Sign-In account and GoogleAuthUtil. */
interface TokenProvider {
    /** Returns a valid OAuth access token for Drive API. */
    @Throws(SecurityException::class)
    fun getAccessToken(): String
    fun hasAccount(): Boolean
}

class GoogleSignInTokenProvider(private val context: Context) : TokenProvider {
    private val scope = "oauth2:https://www.googleapis.com/auth/drive.file"

    override fun hasAccount(): Boolean {
        val acct = GoogleSignIn.getLastSignedInAccount(context)
        return acct?.grantedScopes?.any { it.scopeUri.contains("drive") } == true
    }

    override fun getAccessToken(): String {
        val acct = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw SecurityException("no_signed_in_account")
        val account = Account(acct.email, "com.google")
        return try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: UserRecoverableAuthException) {
            throw SecurityException("user_recoverable:${e.intent?.toString() ?: "unknown"}")
        } catch (e: GooglePlayServicesAvailabilityException) {
            throw SecurityException("gps_unavailable:${e.connectionStatusCode}")
        } catch (e: GooglePlayServicesRepairableException) {
            throw SecurityException("gps_repairable:${e.connectionStatusCode}")
        } catch (e: GooglePlayServicesNotAvailableException) {
            throw SecurityException("gps_not_available")
        } catch (e: GoogleAuthException) {
            throw SecurityException("auth_exception:${e.message}")
        }
    }
}

