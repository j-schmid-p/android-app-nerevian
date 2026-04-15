package com.example.nerevian.data

data class Order(
    val orderNum: String,
    val incoterm: String,
    val cargoType: String,
    val origin: String,
    val destination: String,
    val customer: String,
    val trackingStatusInt: Int?,
    val trackingStatusStr: String?,

    val client: ClientDto?,
    val estat_oferta: EstatOfertaDto?,

)


//TODO mirar si lo de sota te sentit

// DTOs auxiliares para las relaciones
data class ClientDto(
    val id: Int,
    val nom: String,
    val cif: String?
)

data class EstatOfertaDto(
    val id: Int,
    val estat: String
)
