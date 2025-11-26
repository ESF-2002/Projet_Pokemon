package pokemon.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.random.Random
import pokemon.project.model.Pokemon

class Greeting {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun fetchPokemon(): Pokemon? {
        return try {
            val random = Random.nextInt(1, 1026)
            val response: Pokemon = client.get("https://tyradex.vercel.app/api/v1/pokemon/$random").body<Pokemon>()
            response
        } catch (e: Exception) {
            println("Erreur lors de la récupération du Pokémon: ${e.message}")
            null
        }
    }
}
