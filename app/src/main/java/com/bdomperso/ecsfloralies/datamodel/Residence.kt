package com.bdomperso.ecsfloralies.datamodel

import kotlinx.serialization.Serializable

@Serializable
data class Residence(
    val buildings: List<Building>
)