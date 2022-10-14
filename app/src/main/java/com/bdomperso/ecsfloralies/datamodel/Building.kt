package com.bdomperso.ecsfloralies.datamodel

import kotlinx.serialization.Serializable

@Serializable
data class Building(
    val name: String,
    val stages: List<Stage>
)