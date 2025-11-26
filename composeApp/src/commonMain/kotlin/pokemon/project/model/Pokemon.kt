package pokemon.project.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pokemon.project.model.PokemonName
import pokemon.project.model.Sprites

@Serializable
data class Pokemon(
    @SerialName("pokedex_id") val pokedexId: Int? = null,
    val category: String? = null,
    val name: PokemonName? = null,
    val sprites: Sprites? = null
)
