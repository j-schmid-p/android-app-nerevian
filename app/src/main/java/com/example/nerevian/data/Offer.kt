package com.example.nerevian.data

import org.json.JSONObject

data class Offer(
    val id: Int,
    val status: String,
    val cargoType: String?,
    val incoterm: String?,
    val origin: String?,
    val destination: String?,
    val clientName: String?,
    val operatorName: String?,
    val agentName: String?,
    val rejectionReason: String?,
    val dateCreated: String?,
    val comments: String?,
    val grossWeight: Double?,
    val volume: Double?,
    val transportType: String?,
    val flowType: String?,
    val containerType: String?,
    val shippingLine: String?,
    val rawJson: String? = null,
    var isExpanded: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): Offer {
            val statusObj = json.optJSONObject("estat_oferta")
            val status = statusObj?.optString("estat") ?: "Pending"

            val cargoObj = json.optJSONObject("tipus_carrega")
            val cargoType = cargoObj?.optString("tipus")

            val incotermObj = json.optJSONObject("incoterm")
            val incoterm = incotermObj?.optString("nom")

            val portOrigin = json.optJSONObject("port_origen")?.optString("nom")
            val airportOrigin = json.optJSONObject("aeroport_origen")?.optString("nom")
            val origin = portOrigin ?: airportOrigin ?: "N/A"

            val portDest = json.optJSONObject("port_desti")?.optString("nom")
            val airportDest = json.optJSONObject("aeroport_desti")?.optString("nom")
            val destination = portDest ?: airportDest ?: "N/A"

            val clientObj = json.optJSONObject("client")
            val clientName = clientObj?.optString("nom")

            val operatorObj = json.optJSONObject("operador")
            val operatorName = if (operatorObj != null) {
                "${operatorObj.optString("nom")} ${operatorObj.optString("cognoms")}"
            } else null

            val agentObj = json.optJSONObject("agent_comercial")
            val agentName = if (agentObj != null) {
                "${agentObj.optString("nom")} ${agentObj.optString("cognoms")}"
            } else null

            val transportObj = json.optJSONObject("tipus_transport")
            val transportType = transportObj?.optString("tipus")

            val flowObj = json.optJSONObject("tipus_fluxe")
            val flowType = flowObj?.optString("nom")

            val containerObj = json.optJSONObject("tipus_contenidor")
            val containerType = containerObj?.optString("tipus")

            val shippingLineObj = json.optJSONObject("linia_transport_maritim")
            val shippingLine = shippingLineObj?.optString("nom")

            return Offer(
                id = json.getInt("id"),
                status = status,
                cargoType = cargoType,
                incoterm = incoterm,
                origin = origin,
                destination = destination,
                clientName = clientName,
                operatorName = operatorName,
                agentName = agentName,
                rejectionReason = if (json.isNull("rao_rebuig")) null else json.optString("rao_rebuig"),
                dateCreated = if (json.isNull("data_creacio")) null else json.optString("data_creacio"),
                comments = if (json.isNull("comentaris")) null else json.optString("comentaris"),
                grossWeight = if (json.isNull("pes_brut")) null else json.optDouble("pes_brut"),
                volume = if (json.isNull("volum")) null else json.optDouble("volum"),
                transportType = transportType,
                flowType = flowType,
                containerType = containerType,
                shippingLine = shippingLine,
                rawJson = json.toString()
            )
        }
    }
}
