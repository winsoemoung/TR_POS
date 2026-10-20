package com.example.offlinepos.Sync

import android.accounts.Account
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Robust Google OAuth 2.0 & Auto-Sync Token Manager.
 *
 * Utilizes the Google Cloud OAuth client credentials from:
 * project_id: "pos-sync-4"
 * client_id: "365061373735-5j04mcds3apjgn0tbdomfjcp7rorl80d.apps.googleusercontent.com"
 *
 * Supports:
 * 1. OAuth2 Server Auth Code exchange & Refresh Token persistence (Permanent 24/7 background sync)
 * 2. Token refresh before expiry
 * 3. Fallback to GoogleAuthUtil (Play Services account) with client_id scope
 * 4. Browser OAuth2 Consent URL launcher for zero-friction setup
 */
class GoogleOAuthManager(private val context: Context) {

    companion object {
        const val CLIENT_ID = "365061373735-5j04mcds3apjgn0tbdomfjcp7rorl80d.apps.googleusercontent.com"
        const val PROJECT_ID = "pos-sync-4"
        const val AUTH_URI = "https://accounts.google.com/o/oauth2/auth"
        const val TOKEN_URI = "https://oauth2.googleapis.com/token"
        const val USERINFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo"
        const val REDIRECT_URI_OOB = "urn:ietf:wg:oauth:2.0:oob"
        const val REDIRECT_URI_LOCAL = "http://localhost"

        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPES_COMBINED = "https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file email profile"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("pos_google_oauth_tokens", Context.MODE_PRIVATE)

    init {
        val lastClientId = prefs.getString("oauth_client_id_cached", null)
        if (lastClientId != null && lastClientId != CLIENT_ID) {
            clearTokens()
        }
        prefs.edit().putString("oauth_client_id_cached", CLIENT_ID).apply()
    }

    fun isConnected(): Boolean {
        val email = getConnectedEmail()
        val hasToken = !getAccessToken().isNullOrBlank() || !getRefreshToken().isNullOrBlank()
        return !email.isNullOrBlank() || hasToken
    }

    fun getConnectedEmail(): String? = prefs.getString("oauth_user_email", null)

    fun setConnectedEmail(email: String?) {
        prefs.edit().putString("oauth_user_email", email?.trim()?.ifBlank { null }).apply()
    }

    fun getAccessToken(): String? = prefs.getString("oauth_access_token", null)

    fun getRefreshToken(): String? = prefs.getString("oauth_refresh_token", null)

    fun getExpiresAtEpochMs(): Long = prefs.getLong("oauth_expires_at_ms", 0L)

    fun clearTokens() {
        prefs.edit()
            .remove("oauth_access_token")
            .remove("oauth_refresh_token")
            .remove("oauth_expires_at_ms")
            .remove("oauth_user_email")
            .apply()
    }

    /**
     * Builds the Google OAuth 2.0 Web Consent URL.
     * Cashiers or store owners can open this URL in any browser to authorize the app.
     */
    fun buildConsentUrl(redirectUri: String = REDIRECT_URI_OOB, loginHint: String? = null): String {
        val builder = Uri.parse(AUTH_URI).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPES_COMBINED)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")

        if (!loginHint.isNullOrBlank()) {
            builder.appendQueryParameter("login_hint", loginHint)
        }
        return builder.build().toString()
    }

    /**
     * Exchanges an authorization code (from Google Sign-In or browser flow)
     * for access_token and permanent refresh_token.
     */
    suspend fun exchangeAuthorizationCode(
        code: String,
        redirectUri: String = REDIRECT_URI_OOB,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanCode = code.trim()
            require(cleanCode.isNotBlank()) { "Authorization code မရှိပါ" }

            val postData = buildString {
                append("code=").append(URLEncoder.encode(cleanCode, "UTF-8"))
                append("&client_id=").append(URLEncoder.encode(CLIENT_ID, "UTF-8"))
                append("&grant_type=authorization_code")
                append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"))
            }

            val conn = (URL(TOKEN_URI).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(postData); it.flush() }

            val responseCode = conn.responseCode
            val responseBody = (if (responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                error("Token exchange failed ($responseCode): $responseBody")
            }

            val json = JSONObject(responseBody)
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
            val expiresIn = json.optLong("expires_in", 3600L)
            val expiryMs = System.currentTimeMillis() + (expiresIn * 1000L)

            require(accessToken.isNotBlank()) { "No access_token returned" }

            // Fetch user email if possible
            val email = fetchUserEmail(accessToken) ?: getConnectedEmail() ?: "WINSOEMOUNG@gmail.com"

            prefs.edit().apply {
                putString("oauth_access_token", accessToken)
                if (refreshToken != null) {
                    putString("oauth_refresh_token", refreshToken)
                }
                putLong("oauth_expires_at_ms", expiryMs)
                putString("oauth_user_email", email)
                apply()
            }

            RealtimeDriveSyncCoordinator.clearAuthRecoveryIntent()
            email
        }
    }

    /**
     * Refreshes the access_token using the stored refresh_token.
     */
    suspend fun refreshAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val refreshToken = getRefreshToken() ?: error("Refresh token မရှိပါ")
            val postData = buildString {
                append("client_id=").append(URLEncoder.encode(CLIENT_ID, "UTF-8"))
                append("&refresh_token=").append(URLEncoder.encode(refreshToken, "UTF-8"))
                append("&grant_type=refresh_token")
            }

            val conn = (URL(TOKEN_URI).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(postData); it.flush() }

            val responseCode = conn.responseCode
            val responseBody = (if (responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                error("Token refresh failed ($responseCode): $responseBody")
            }

            val json = JSONObject(responseBody)
            val newAccessToken = json.optString("access_token")
            val expiresIn = json.optLong("expires_in", 3600L)
            val expiryMs = System.currentTimeMillis() + (expiresIn * 1000L)

            require(newAccessToken.isNotBlank()) { "No access_token in refresh response" }

            prefs.edit()
                .putString("oauth_access_token", newAccessToken)
                .putLong("oauth_expires_at_ms", expiryMs)
                .apply()

            newAccessToken
        }
    }

    /**
     * Returns a valid access token for Google Drive API calls.
     * 1. Returns existing token if still valid (with 60-second buffer).
     * 2. Refreshes using refresh_token if expired.
     * 3. Falls back to GoogleAuthUtil (Play Services Account) if available.
     */
    suspend fun getValidAccessToken(preferredEmail: String? = null): String = withContext(Dispatchers.IO) {
        // 1. Check cached OAuth token
        val cachedToken = getAccessToken()
        val expiresAt = getExpiresAtEpochMs()
        val now = System.currentTimeMillis()

        if (!cachedToken.isNullOrBlank() && expiresAt > now + 60_000L) {
            return@withContext cachedToken
        }

        // 2. Try refresh_token
        if (!getRefreshToken().isNullOrBlank()) {
            val refreshResult = refreshAccessToken()
            if (refreshResult.isSuccess) {
                return@withContext refreshResult.getOrThrow()
            }
        }

        // 3. Fallback to GoogleAuthUtil
        val email = preferredEmail?.trim()?.ifBlank { null }
            ?: getConnectedEmail()
            ?: error("Google Account ချိတ်ဆက်ထားခြင်း မရှိပါ")

        val account = Account(email, "com.google")

        // Try with server client ID scope first
        try {
            val tokenWithClient = GoogleAuthUtil.getToken(
                context,
                account,
                "oauth2:server:client_id:$CLIENT_ID:api_scope:$SCOPES_COMBINED"
            )
            cacheGoogleAuthUtilToken(tokenWithClient, email)
            RealtimeDriveSyncCoordinator.clearAuthRecoveryIntent()
            return@withContext tokenWithClient
        } catch (ex: UserRecoverableAuthException) {
            RealtimeDriveSyncCoordinator.setAuthRecoveryIntent(ex.intent)
            throw ex
        } catch (_: Exception) {
            // Fallback to direct scopes
        }

        try {
            val tokenCombined = GoogleAuthUtil.getToken(context, account, "oauth2:$SCOPES_COMBINED")
            cacheGoogleAuthUtilToken(tokenCombined, email)
            RealtimeDriveSyncCoordinator.clearAuthRecoveryIntent()
            return@withContext tokenCombined
        } catch (ex: UserRecoverableAuthException) {
            RealtimeDriveSyncCoordinator.setAuthRecoveryIntent(ex.intent)
            throw ex
        } catch (_: Exception) {
            // Fallback to appdata only
        }

        val tokenAppData = GoogleAuthUtil.getToken(context, account, "oauth2:$SCOPE_DRIVE_APPDATA")
        cacheGoogleAuthUtilToken(tokenAppData, email)
        RealtimeDriveSyncCoordinator.clearAuthRecoveryIntent()
        tokenAppData
    }

    private fun cacheGoogleAuthUtilToken(token: String, email: String) {
        prefs.edit()
            .putString("oauth_access_token", token)
            .putLong("oauth_expires_at_ms", System.currentTimeMillis() + 3000_000L) // 50 mins
            .putString("oauth_user_email", email)
            .apply()
    }

    private fun fetchUserEmail(accessToken: String): String? {
        return try {
            val conn = (URL(USERINFO_URI).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optString("email").takeIf { it.isNotBlank() }
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
