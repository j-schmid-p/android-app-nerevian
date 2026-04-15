package com.example.nerevian.network

import android.icu.util.Output
import android.widget.Toast
import com.google.api.Http
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiService {

    //IP local
    //private val BASE_URL = "http://127.0.0.1:8000/api"


    //IP si es corre en emulador de android studio
    //private val BASE_URL = "http://10.0.2.2:8000/api"


    //IP si es corre amb movil endollat
    // (canviar IP per la del PC que s'estigui fent servir)
    private val BASE_URL = "http://192.168.1.48:8000/api"


    fun login(email: String, password: String) : JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/auth/login", "POST", null)

            val body = JSONObject().apply {
                put("correu", email)
                put("contrasenya", password)
            }

            writeBody(connection, body)
            readResponse(connection)
        } catch (e: Exception) { null }
    }

    fun getMe(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/auth/me", "GET", token)
            readResponse(connection)
        } catch (e: Exception) { null }
    }

    fun getProfile(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/profile", "GET", token)
            readResponse(connection)
        } catch (e: Exception) { null }
    }

    fun getOrders(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/orders", "GET", token)
            readResponse(connection)
        } catch (e: Exception) { null }
    }

    fun getTracking(token: String, offerId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking", "GET", token)
            readResponse(connection)
        } catch (e: Exception) {
            null
        }
    }

    fun updateProfile(
        token: String,
        nom: String? = null,
        cognoms: String? = null
/* , birthdate: String? = null */
    ): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/profile", "PUT", token)

            val body = JSONObject().apply {
                if (nom != null) put("nom", nom)
                if (cognoms != null) put("cognoms", cognoms)
                // if (birthdate != null) put("data_naixement", birthdate)
            }

            writeBody(connection, body)
            readResponse(connection)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            null }
    }

    fun getOffers(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers", "GET", token)
            readResponse(connection)
        } catch (e: Exception) { null }
    }

    fun logout(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/auth/logout", "POST", token)
            readResponse(connection)
        } catch (e: Exception) { null }
    }

    private fun openConnection(url: String, method: String, token: String?) : HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        if (token != null) { connection.setRequestProperty("Authorization", "Bearer $token") }
        if (method == "POST" || method == "PUT"){ connection.doOutput = true }
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        return connection
    }

    private fun writeBody(connection: HttpURLConnection, body: JSONObject){
        OutputStreamWriter(connection.outputStream).use {
            it.write(body.toString())
            it.flush()
        }
    }

    private fun readResponse(connection: HttpURLConnection): JSONObject? {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else { connection.errorStream }

        val response = stream?.bufferedReader()?.readText() ?: ""
        connection.disconnect()

        return if (response.isNotEmpty()) { JSONObject(response) } else { null }
    }
}
