package com.example.gascalc

data class distanceQuery(
    val destination_addresses : List<String>,
    val origin_addresses : List<String>,
    val rows : List<String>
)
