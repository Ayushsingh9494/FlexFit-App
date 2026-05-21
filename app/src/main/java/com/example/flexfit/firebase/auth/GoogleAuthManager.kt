package com.example.flexfit.firebase.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(context: Context) {

    private val appContext = context.applicationContext
    private val webClientId = appContext.resources
        .getIdentifier("default_web_client_id", "string", appContext.packageName)
        .takeIf { it != 0 }
        ?.let(appContext::getString)
        ?: error("Missing default_web_client_id. Re-download google-services.json from Firebase.")

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    private val googleClient: GoogleSignInClient = GoogleSignIn.getClient(appContext, signInOptions)

    fun signInIntent(): Intent = googleClient.signInIntent

    suspend fun extractIdToken(intent: Intent?): Result<String> {
        return runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(intent).await()
            account.idToken ?: error("Missing Google ID token.")
        }.recoverCatching { throwable ->
            if (throwable is ApiException && throwable.statusCode == 12501) {
                error("Google sign-in was cancelled.")
            }
            throw throwable
        }
    }

    suspend fun signOut() {
        googleClient.signOut().await()
        googleClient.revokeAccess().await()
    }
}
