package com.example.nerevian.utils

import android.content.Context

class SessionManager(private val context: Context) {

    private val preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    val token: String? get() = preferences.getString("token", null)
    val userId: Int get() = preferences.getInt("id", -1)
    val email: String get() = preferences.getString("correu", "") ?: ""
    val name: String get() = preferences.getString("nom", "") ?: ""
    val lastName: String get() = preferences.getString("cognoms", "") ?: ""
    val rolId: Int get() = preferences.getInt("rol_id", -1)
    val clientId: Int get() = preferences.getInt("client_id", -1)


    fun logout() { preferences.edit().clear().apply() }

    fun updateUserInfo(name: String, lastName: String) {
        preferences.edit()
            .putString("nom", name)
            .putString("cognoms", lastName)
            .apply()
    }
}