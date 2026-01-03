package com.smartlighting.mobile.data.api

import com.smartlighting.mobile.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor for API requests
 * Note: Authentication uses cookies (handled by CookieJar), not headers
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Backend uses session cookies for authentication
        // Cookies are automatically handled by OkHttpClient's CookieJar
        // No need to add Authorization headers
        
        return chain.proceed(originalRequest)
    }
}

