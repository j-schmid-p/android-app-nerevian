package com.example.nerevian.network

import android.util.Log
import com.example.nerevian.data.Offer
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiService {

    private val TAG = "ApiService"

    //IP local
    //private val BASE_URL = "http://127.0.0.1:8000/api"


    //IP si es corre en emulador de android studio
    //private val BASE_URL = "http://10.0.2.2:8000/api"


    //IP si es corre amb movil endollat
    // (canviar IP per la del PC que s'estigui fent servir)
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
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            null
        }
    }

    fun getMe(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/auth/me", "GET", token)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "getMe error: ${e.message}", e)
            null
        }
    }

    fun updateProfile(
        token: String,
        nom: String? = null,
        cognoms: String? = null
    ): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/profile", "PUT", token)
            val body = JSONObject().apply {
                if (nom != null) put("nom", nom)
                if (cognoms != null) put("cognoms", cognoms)
            }
            writeBody(connection, body)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile error: ${e.message}", e)
            null
        }
    }

    fun getTrackingOptions(token: String, offerId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking", "GET", token)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "getTrackingOptions error: ${e.message}", e)
            null
        }
    }

    fun getCurrentTracking(token: String, offerId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking/current", "GET", token)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentTracking error: ${e.message}", e)
            null
        }
    }

    fun getOffersList(token: String): List<Offer>? {
        return try {
            val connection = openConnection("$BASE_URL/offers", "GET", token)
            val jsonArray = readJSONArray(connection)

            if (jsonArray != null) {
                val offersList = mutableListOf<Offer>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    offersList.add(Offer.fromJson(item))
                }
                offersList
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getOffersList error: ${e.message}", e)
            null
        }
    }

    fun logout(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/auth/logout", "POST", token)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "logout error: ${e.message}", e)
            null
        }
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

    private fun readJSONObject(connection: HttpURLConnection): JSONObject? {
        val response = readRawResponse(connection)
        return if (response.isNotEmpty()) { JSONObject(response) } else { null }
    }

    private fun readJSONArray(connection: HttpURLConnection): JSONArray? {
        val response = readRawResponse(connection)
        return if (response.isNotEmpty()) { JSONArray(response) } else { null }
    }

    private fun readRawResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val response = stream?.bufferedReader()?.readText() ?: ""
        connection.disconnect()
        return response
    }
}
