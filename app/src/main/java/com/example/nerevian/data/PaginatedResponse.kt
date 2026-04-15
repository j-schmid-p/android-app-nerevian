package com.example.nerevian.data


//TODO mirar si te sentit aquesta classe
data class PaginatedResponse<T>(
    val current_page: Int,
    val data: List<T>,
    val last_page: Int,
    val per_page: Int,
    val total: Int
)
