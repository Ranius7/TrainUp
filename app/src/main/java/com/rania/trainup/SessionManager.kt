package com.rania.trainup

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("trainup_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_EMAIL = "KEY_EMAIL"
        const val KEY_ROLE = "KEY_ROLE"
        const val KEY_UID = "KEY_UID" // ¡IMPORTANTE! Añadimos el UID
    }

    fun saveSession(email: String, role: String, uid: String) {
        prefs.edit().apply {
            putString(KEY_EMAIL, email)
            putString(KEY_ROLE, role)
            putString(KEY_UID, uid)
            apply()
        }
    }

    fun getSession(): Triple<String?, String?, String?> { // Devuelve email, rol, UID
        val email = prefs.getString(KEY_EMAIL, null)
        val role = prefs.getString(KEY_ROLE, null)
        val uid = prefs.getString(KEY_UID, null)
        return Triple(email, role, uid)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}