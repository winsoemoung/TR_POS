package com.example.offlinepos.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.offlinepos.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    private fun getAuth(): FirebaseAuth = Firebase.auth

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

    fun clearError() {
        _authError.value = null
    }

    fun getWebClientId(context: Context): String {
        return try {
            context.getString(R.string.default_web_client_id)
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
                    _authError.value = null
                    _isSigningIn.value = false
                    Log.d(TAG, "Google Sign-In success: ${authResult.user?.email}")
                    onComplete(true, null)
                } else {
                    _isSigningIn.value = false
                    val msg = "Unexpected credential format received (${credential.type})"
                    _authError.value = msg
                    onComplete(false, msg)
                }
            } catch (e: GetCredentialCancellationException) {
                val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                    ?: e.message?.takeIf { it.isNotBlank() }
                    ?: "Cancelled / Dismissed"
                Log.w(TAG, "Google Sign-In cancelled or dismissed: $detail", e)
                _isSigningIn.value = false
                val msg = "Google Sign-In ပယ်ဖျက်ခံရပါသည် ($detail) — စက်ထဲတွင် Google Account ချိတ်ဆက်ထားခြင်း ရှိ/မရှိ နှင့် Firebase Console SHA-1 Key ကိုက်ညီမှု စစ်ဆေးပါ။"
                _authError.value = msg
                onComplete(false, msg)
            } catch (e: NoCredentialException) {
                val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: "NoCredentialException"
                Log.w(TAG, "No Google credential available: $detail", e)
                _isSigningIn.value = false
                val msg = "စက်ထဲတွင် Google Account ချိတ်ဆက်ထားခြင်း မတွေ့ရှိပါ ($detail) — ဖုန်း Settings > Accounts ထဲတွင် Gmail ထည့်သွင်းပြီး ထပ်မံစမ်းသပ်ပါ။"
                _authError.value = msg
                onComplete(false, msg)
            } catch (e: GetCredentialException) {
                val detail = listOfNotNull(
                    e.type.takeIf { it.isNotBlank() },
                    e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.message?.takeIf { it.isNotBlank() }
                ).joinToString(": ")
                Log.e(TAG, "Credential Manager error: $detail", e)
                _isSigningIn.value = false
                val msg = "Google Credential ရယူ၍ မရပါ ($detail)"
                _authError.value = msg
                onComplete(false, msg)
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "FirebaseAuth error [${e.errorCode}]: ${e.localizedMessage}", e)
                _isSigningIn.value = false
                val msg = "Firebase Login မအောင်မြင်ပါ [${e.errorCode}]: ${e.localizedMessage ?: "စစ်ဆေးပါ"}"
                _authError.value = msg
                onComplete(false, msg)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error", e)
                _isSigningIn.value = false
                val msg = "Google Sign-In မအောင်မြင်ပါ: ${e.localizedMessage ?: e.javaClass.simpleName}"
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
                    _authError.value = null
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
        _authError.value = null
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
