package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

tailrec fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

suspend fun doGoogleSignIn(context: Context): String? {
    val activityContext = context.getActivity() ?: context
    val credentialManager = CredentialManager.create(activityContext)

    try {
        val WEB_CLIENT_ID = com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activityContext,
        )
        val credential = result.credential
        
        if (credential is GoogleIdTokenCredential) {
            // If we get here, login was successful
            Log.d("GoogleAuth", "Login success! ID: ${credential.id}")
            return credential.id
        } else {
            Log.d("GoogleAuth", "Unexpected type of credential")
            return null
        }
    } catch (e: GetCredentialException) {
        Log.e("GoogleAuth", "GetCredentialException", e)
        return null
    } catch (e: Exception) {
        Log.e("GoogleAuth", "Unexpected error", e)
        return null
    }
}
