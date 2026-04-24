package com.example.nerevian.network

import android.util.Log
import com.example.nerevian.data.Offer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
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

    fun updateTrackingStep(token: String, offerId: Int, stepId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId/tracking", "POST", token)
            // Some Laravel setups prefer standard PUT/PATCH if the route allows, 
            // but your controller uses $request->validate inside updateTrackingStep.
            // Let's use POST with X-HTTP-Method-Override PATCH to be safe
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            val body = JSONObject().apply {
                put("tracking_step_id", stepId)
            }
            writeBody(connection, body)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "updateTrackingStep error: ${e.message}", e)
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

    fun updateOfferStatus(token: String, offerId: Int, statusId: Int): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/offers/$offerId", "PUT", token)
            val body = JSONObject().apply {
                put("estat_oferta_id", statusId)
            }
            writeBody(connection, body)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "updateOfferStatus error: ${e.message}", e)
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

    /**
     * Puja un fitxer DNI al servidor amb multipart/form-data.
     * El camp ha de dir-se exactament "dni".
     *
     * @param token  Bearer token de Sanctum
     * @param file   Fitxer local a pujar (PDF, imatge…)
     * @param mimeType  p.ex. "application/pdf" o "image/jpeg"
     * @return JSONObject amb { message, document: { name, path, download_url }, path }
     *         o null si hi ha error de xarxa
     */
    fun uploadDni(token: String, file: File, mimeType: String): JSONObject? {
        return try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("$BASE_URL/profile/dni")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }

            connection.outputStream.use { outputStream ->
                val writer = outputStream.bufferedWriter(Charsets.UTF_8)

                // Part del fitxer: camp "dni"
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"dni\"; filename=\"${file.name}\"\r\n")
                writer.write("Content-Type: $mimeType\r\n")
                writer.write("\r\n")
                writer.flush()

                file.inputStream().use { it.copyTo(outputStream) }
                outputStream.flush()

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.readText() ?: ""
            connection.disconnect()

            Log.d(TAG, "uploadDni [$responseCode]: $raw")
            if (raw.isNotEmpty()) JSONObject(raw) else null

        } catch (e: Exception) {
            Log.e(TAG, "uploadDni error: ${e.message}", e)
            null
        }
    }

    /**
     * Obté les metadades del DNI de l'usuari actual i una URL signada per descarregar-lo.
     *
     * @return JSONObject amb { document: { name, path, download_url } } o { document: null }
     *         Retorna null si hi ha error de xarxa.
     */
    fun getDniMetadata(token: String): JSONObject? {
        return try {
            val connection = openConnection("$BASE_URL/profile/dni", "GET", token)
            readJSONObject(connection)
        } catch (e: Exception) {
            Log.e(TAG, "getDniMetadata error: ${e.message}", e)
            null
        }
    }

    /**
     * Descarrega el fitxer DNI des de la URL signada retornada pel servidor.
     * NO usa el Bearer token (la URL signada ja porta autenticació).
     *
     * @param signedUrl   Valor de document.download_url retornat per getDniMetadata()
     * @param destination Fitxer local on guardar el contingut
     * @throws IOException si la descàrrega falla o la resposta és buida
     */
    @Throws(IOException::class)
    fun downloadDniFile(signedUrl: String, destination: File) {
        val connection = (URL(signedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 30000
        }

        val responseCode = connection.responseCode
        Log.d(TAG, "downloadDniFile [$responseCode] -> ${destination.absolutePath}")

        if (responseCode !in 200..299) {
            connection.disconnect()
            throw IOException("La descàrrega ha fallat amb codi $responseCode")
        }

        val body = connection.inputStream
            ?: run { connection.disconnect(); throw IOException("Resposta buida del servidor") }

        destination.outputStream().use { output ->
            body.copyTo(output)
        }
        connection.disconnect()
    }

    private fun openConnection(url: String, method: String, token: String?): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        if (token != null) { connection.setRequestProperty("Authorization", "Bearer $token") }
        if (method == "POST" || method == "PUT") { connection.doOutput = true }
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        return connection
    }

    private fun writeBody(connection: HttpURLConnection, body: JSONObject) {
        OutputStreamWriter(connection.outputStream).use {
            it.write(body.toString())
            it.flush()
        }
    }

    fun readJSONObject(connection: HttpURLConnection): JSONObject? {
        val response = readRawResponse(connection)
        if (response.isEmpty()) return null
        return try {
            val trimmed = response.trim()
            if (trimmed.startsWith("[")) {
                // If it's an array, wrap it in a "data" object for backward compatibility
                JSONObject().put("data", JSONArray(trimmed))
            } else {
                JSONObject(trimmed)
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error parsing JSONObject: ${e.message}", e)
            null
        }
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
        Log.d(TAG, "Request: ${connection.requestMethod} ${connection.url} | Code: ${connection.responseCode} | Response: $response")
        connection.disconnect()
        return response
    }
}
