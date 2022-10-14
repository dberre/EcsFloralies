package com.bdomperso.ecsfloralies.datamodel

import kotlinx.serialization.Serializable

@Serializable
data class Apartment(
    val counter1: String,
    val counter2: String,
    val type: String
)