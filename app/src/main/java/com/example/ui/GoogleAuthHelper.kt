package com.example.ui

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun doGoogleSignIn(context: Context): String? {
    return withContext(Dispatchers.IO) {
        val credentialManager = CredentialManager.create(context)

        // Web Client ID must be configured in Google Cloud console for your SHA-1.
        // As a fallback for demo purposes where setup is missing, we use a placeholder or check config.
        // It's recommended to place the OAuth Web Client ID here.
        val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE" // Please replace with actual Web Client ID.

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            val credential = result.credential
            
            if (credential is GoogleIdTokenCredential) {
                // If we get here, login was successful
                Log.d("GoogleAuth", "Login success! ID: ${credential.id}")
                return@withContext credential.id
            } else {
                Log.d("GoogleAuth", "Unexpected type of credential")
                return@withContext null
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "GetCredentialException", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Unexpected error", e)
            return@withContext null
        }
    }
}
