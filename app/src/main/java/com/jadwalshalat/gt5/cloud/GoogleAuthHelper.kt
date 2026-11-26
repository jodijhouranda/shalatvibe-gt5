package com.jadwalshalat.gt5.cloud

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jadwalshalat.gt5.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

private const val AUTH_TAG = "Auth"
private const val REGISTERED_DEBUG_SHA1 = "D6:30:AB:BD:16:E6:4E:D6:FC:D9:A4:DA:8D:26:F8:57:AA:AF:FC:69"

fun getAppSigningSha1(context: Context): String? {
    return runCatching {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
            if (signingInfo == null) null
            else if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
            else signingInfo.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching null
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString(":") { b -> "%02X".format(b) }
    }.getOrNull()
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun resolveDefaultWebClientId(context: Context): String? {
    return runCatching { context.getString(R.string.default_web_client_id) }
        .getOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun resolveFirebaseAuth(context: Context): FirebaseAuth? {
    if (FirebaseApp.getApps(context).isEmpty()) {
        runCatching { FirebaseApp.initializeApp(context) }
    }
    if (FirebaseApp.getApps(context).isEmpty()) return null
    return runCatching { FirebaseAuth.getInstance() }.getOrNull()
}

/**
 * Silent auto sign-in on startup using GetGoogleIdOption only (1 credential option per request).
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val firebaseAuth = resolveFirebaseAuth(context)
    if (firebaseAuth == null) {
        onUnauthenticated()
        return
    }
    if (firebaseAuth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption only (1 credential option per request).
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String?) -> Unit = {}
) {
    val firebaseAuth = resolveFirebaseAuth(context)
    if (firebaseAuth == null) {
        onAuthError("Firebase belum aktif pada build APK ini (google-services.json tidak disertakan).")
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onAuthError("Konfigurasi Google Sign-In belum tersedia. Perbarui google-services.json (OAuth Web Client ID) lalu build ulang APK.")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Konteks Activity tidak tersedia untuk membuka login Google.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val authResult = firebaseAuth
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                onAuthSuccess(authResult.user ?: firebaseAuth.currentUser)
            } else {
                onAuthError("Tipe kredensial Google tidak dikenali.")
            }
        } catch (e: GetCredentialCancellationException) {
            val detail = e.errorMessage?.toString().orEmpty()
            val sha1 = getAppSigningSha1(context)
            val isUnregisteredReleaseKey = sha1 != null && !sha1.equals(REGISTERED_DEBUG_SHA1, ignoreCase = true)
            Log.w(AUTH_TAG, "Google Sign-In cancelled/dismissed: $detail | sha1=$sha1", e)
            if (isUnregisteredReleaseKey) {
                onAuthError(
                    "Login Google ditolak karena SHA-1 keystore APK rilis ini belum didaftarkan di Firebase Console (gen-lang-client-0567297449). Salin SHA-1 di bawah lalu tambahkan di Firebase → Project Settings → Add fingerprint."
                )
            } else {
                val hint = detail.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                onAuthCancelled("Login Google dibatalkan atau ditolak Play Services$hint.")
            }
        } catch (e: NoCredentialException) {
            val sha1 = getAppSigningSha1(context)
            Log.w(AUTH_TAG, "No Google credential available: ${e.errorMessage} | sha1=$sha1", e)
            onAuthError("Akun Google belum tersedia di perangkat ini atau SHA-1 APK belum terdaftar di Firebase Console.")
        } catch (e: GetCredentialException) {
            Log.e(AUTH_TAG, "CredentialManager error [${e.type}]: ${e.errorMessage}", e)
            val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                ?: e.localizedMessage
                ?: "Gagal membuka login Google."
            onAuthError("Gagal login Google: $detail")
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Gagal masuk dengan Google.")
        }
    }
}

/**
 * Safe sign-out: detaches active listeners and clears Credential Manager state in NonCancellable
 * BEFORE signing out of FirebaseAuth to prevent PERMISSION_DENIED races.
 */
fun signOutGoogle(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeSignOut: () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            onBeforeSignOut()
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { FirebaseAuth.getInstance() }.getOrNull()?.signOut()
                onSignOutComplete()
            }
        }
    }
}
