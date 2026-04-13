package com.example.nerevian.data

data class Order(
    val orderNum: String,
    val status: String,
    val incoterm: String,
    val cargo: String,
    val origin: String,
    val destination: String,
    val customer: String


)