package com.example.nerevian.utils

import android.content.Context

class SessionManager(private val context: Context) {

    private val preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    val token: String? get() = preferences.getString("token", null)
    //TODO saber si envia user_id
    val userId: Int get() = preferences.getInt("user_id", -1)
    val rolId: Int get() = preferences.getInt("rol_id", -1)
    val name: String get() = preferences.getString("nom", "") ?: ""
    val lastName: String get() = preferences.getString("cognoms", "") ?: ""
    val email: String get() = preferences.getString("correu", "") ?: ""

    fun logout() { preferences.edit().clear().apply() }
}