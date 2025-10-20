package pokemon.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pokemon(
    @SerialName("pokedex_id") val pokedexId: Int? = null,
    val category: String? = null,
    val name: PokemonName? = null,
    val sprites: Sprites? = null
)
