package pokemon.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Sprites(
    val regular: String? = null,
    val shiny: String? = null,
    val gmax: Gmax? = null
)

@Serializable
data class Gmax(
    val regular: String? = null,
    val shiny: String? = null
)
