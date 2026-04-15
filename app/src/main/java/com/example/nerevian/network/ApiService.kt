package com.example.nerevian.network

import android.icu.util.Output
import android.widget.Toast
import com.example.nerevian.data.Offer
import com.google.android.gms.common.api.Response
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
    private val BASE_URLL = "https://webhook.site/340d8a1d-343f-48f8-8c77-db3624ec5bf4"


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
            //val connection = openConnection(BASE_URLL, "GET", token)
            readResponse(connection)
        } catch (e: Exception) { null }
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
            readResponse(connection)
        } catch (e: Exception) {
            null
        }
    }

    fun getTrackingOptions(token: String, offerId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking", "GET", token)
            readResponse(connection)
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentTracking(token: String, offerId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking/current", "GET", token)
            readResponse(connection)
        } catch (e: Exception) {
            null
        }
    }

    fun patchTracking(token: String, offerId: Int, stepId: Int): JSONObject? {
        return try {
            // Some Android versions don't support PATCH in HttpURLConnection directly.
            // We use POST with a method override or try PATCH if supported.
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking", "PATCH", token)
            val body = JSONObject().apply {
                put("current_tracking_step_id", stepId)
            }
            writeBody(connection, body)
            readResponse(connection)
        } catch (e: Exception) {
            null
        }
    }

    fun getOffersList(token: String): List<Offer>? {
        return try {
            val connection = openConnection("$BASE_URL/offers", "GET", token)

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.readText() ?: ""
            connection.disconnect()

            if (response.isNotEmpty()) {
                // Como es una lista, lo parseamos como JSONArray directamente
                val jsonArray = org.json.JSONArray(response)
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
            e.printStackTrace()
            null
        }
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
