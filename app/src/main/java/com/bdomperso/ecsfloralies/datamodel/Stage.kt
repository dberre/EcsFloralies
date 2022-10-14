package com.bdomperso.ecsfloralies.datamodel

import kotlinx.serialization.Serializable

@Serializable
data class Stage(
    val apartments: List<Apartment>,
    val level: Int
)