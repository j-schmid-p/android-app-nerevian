package com.example.nerevian.data

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class Offer(
    val id: Int,
    @SerializedName("id") val numero: String?, // Ajusta según el JSON real
    val status: String,
    @SerializedName("client_id") val clientId: Int,
    val comentarios: String?,
    @SerializedName("data_creacio") val fecha: String,
    // Datos para el desplegable
    val incoterm: String?,
    @SerializedName("cargo_type") val cargoType: String?,
    // Variable para el control de la UI (no viene de la API)
    var isExpanded: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): Offer {
            return Offer(
                id = json.getInt("id"),
                numero = json.optString("numero_oferta", "N/A"),
                status = json.getString("status"),
                clientId = json.getInt("client_id"),
                comentarios = json.optString("comentaris"),
                fecha = json.getString("data_creacio"),
                incoterm = json.optJSONObject("incoterm")?.optString("nom"),
                cargoType = json.optJSONObject("tipus_carrega")?.optString("nom")
            )
        }
    }
}
