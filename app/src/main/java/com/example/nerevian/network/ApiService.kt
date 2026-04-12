package com.example.nerevian.network

import android.icu.util.Output
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiService {

    private val BASE_URL = "http://127.0.0.1:8000/api"

    fun login(email: String, password: String) : JSONObject? {
        return try {
            val url = URL("$BASE_URL/auth/login")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 1000
            connection.readTimeout = 1000

            val body = JSONObject().apply {
                put("correu", email)
                put("contrasenya", password)
            }

            OutputStreamWriter(connection.outputStream).use {
                it.write(body.toString())
                it.flush()
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            JSONObject(response)
        } catch (e: Exception) {
            null
        }
    }
}