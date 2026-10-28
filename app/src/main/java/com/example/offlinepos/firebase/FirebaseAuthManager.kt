package com.example.offlinepos.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.offlinepos.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    private fun getAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    fun init(context: Context) {
        val auth = getAuth()
        _currentUser.value = auth.currentUser
        if (authListener == null) {
            val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                _currentUser.value = fbAuth.currentUser
                Log.d(TAG, "Auth state updated: ${fbAuth.currentUser?.email ?: "Unauthenticated"}")
            }
            auth.addAuthStateListener(listener)
            authListener = listener
        }
    }

    fun getWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val clientId = getWebClientId(activity)
        if (clientId.isEmpty()) {
            Log.e(TAG, "Web Client ID not found in resources")
            val msg = "Google Sign-In configuration error: default_web_client_id not found. Firebase Console တွင် Google Authentication ကို enable ပြုလုပ်ပေးပါ။"
            _authError.value = msg
            onComplete(false, msg)
            return
        }

        _isSigningIn.value = true
        _authError.value = null

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch(Dispatchers.Main) {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = getAuth().signInWithCredential(authCredential).await()
                    _currentUser.value = authResult.user
                    _isSigningIn.value = false
                    Log.d(TAG, "Google Sign-In success: ${authResult.user?.email}")
                    onComplete(true, null)
                } else {
                    _isSigningIn.value = false
                    val msg = "Unexpected credential format received"
                    _authError.value = msg
                    onComplete(false, msg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                _isSigningIn.value = false
                onComplete(false, "Sign-in was cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error", e)
                _isSigningIn.value = false
                val msg = e.localizedMessage ?: "Google Sign-In failed"
                _authError.value = msg
                onComplete(false, msg)
            }
        }
    }

    fun attemptAutoSignIn(context: Context, scope: CoroutineScope) {
        val auth = getAuth()
        if (auth.currentUser != null) {
            _currentUser.value = auth.currentUser
            return
        }

        val clientId = getWebClientId(context)
        if (clientId.isEmpty()) {
            return
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        scope.launch(Dispatchers.IO) {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = getAuth().signInWithCredential(authCredential).await()
                    _currentUser.value = authResult.user
                    Log.d(TAG, "Silent auto-sign-in succeeded for ${authResult.user?.email}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto-sign-in skipped: ${e.message}")
            }
        }
    }

    fun signOut(context: Context, scope: CoroutineScope, onComplete: () -> Unit = {}) {
        getAuth().signOut()
        _currentUser.value = null
        val credentialManager = CredentialManager.create(context)
        scope.launch(Dispatchers.IO) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing credential state", e)
            } finally {
                launch(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
