package com.smartlighting.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent CookieJar that stores cookies in SharedPreferences
 * This ensures session cookies persist across app restarts
 */
@Singleton
class PersistentCookieJar @Inject constructor(
    private val context: Context
) : CookieJar {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "cookie_prefs",
        Context.MODE_PRIVATE
    )

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        // Load cookies from SharedPreferences on initialization
        loadCookies()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        
        // Remove existing cookies for this host
        cookieStore[host] = cookies.toMutableList()
        
        // Persist to SharedPreferences
        saveCookies()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: emptyList()
        
        // Remove expired cookies
        val validCookies = cookies.filter { !it.expiresAt.isBefore(System.currentTimeMillis()) }
        
        if (validCookies.size != cookies.size) {
            cookieStore[host] = validCookies.toMutableList()
            saveCookies()
        }
        
        return validCookies
    }

    /**
     * Clear all cookies
     */
    fun clear() {
        cookieStore.clear()
        prefs.edit().clear().apply()
    }

    /**
     * Save cookies to SharedPreferences
     */
    private fun saveCookies() {
        val editor = prefs.edit()
        editor.clear()
        
        for ((host, cookies) in cookieStore) {
            val cookieStrings = cookies.map { serializeCookie(it) }
            editor.putStringSet("cookies_$host", cookieStrings.toSet())
        }
        
        editor.apply()
    }

    /**
     * Load cookies from SharedPreferences
     */
    private fun loadCookies() {
        cookieStore.clear()
        
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            if (key.startsWith("cookies_")) {
                val host = key.substring("cookies_".length)
                val cookieStrings = prefs.getStringSet(key, emptySet()) ?: emptySet()
                val cookies = cookieStrings.mapNotNull { deserializeCookie(it) }
                cookieStore[host] = cookies.toMutableList()
            }
        }
    }

    /**
     * Serialize cookie to string
     */
    private fun serializeCookie(cookie: Cookie): String {
        return buildString {
            append(cookie.name)
            append("|")
            append(cookie.value)
            append("|")
            append(cookie.expiresAt)
            append("|")
            append(cookie.domain)
            append("|")
            append(cookie.path)
            append("|")
            append(cookie.secure)
            append("|")
            append(cookie.httpOnly)
            append("|")
            append(cookie.hostOnly)
        }
    }

    /**
     * Deserialize cookie from string
     */
    private fun deserializeCookie(serialized: String): Cookie? {
        return try {
            val parts = serialized.split("|")
            if (parts.size != 8) return null
            
            Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .expiresAt(parts[2].toLong())
                .domain(parts[3])
                .path(parts[4])
                .apply {
                    if (parts[5].toBoolean()) secure()
                    if (parts[6].toBoolean()) httpOnly()
                    if (parts[7].toBoolean()) hostOnlyDomain(parts[3])
                }
                .build()
        } catch (e: Exception) {
            null
        }
    }

    private fun Long.isBefore(currentTime: Long): Boolean {
        return this < currentTime
    }
}

