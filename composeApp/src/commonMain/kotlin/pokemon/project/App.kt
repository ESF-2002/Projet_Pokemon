package pokemon.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun App() {
    var showContent by remember { mutableStateOf(false) }
    var pokemon by remember { mutableStateOf<Pokemon?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(showContent) {
        if (showContent) {
            isLoading = true
            pokemon = Greeting().fetchPokemon()
            isLoading = false
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showContent = !showContent },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Chargement..." else "Charger un Pokémon")
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                }

                pokemon?.let { poke ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = poke.name?.fr ?: "Inconnu",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Catégorie: ${poke.category ?: "Inconnue"}",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = "Pokédex #${poke.pokedexId ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            poke.sprites?.regular?.let { imageUrl ->
                                KamelImage(
                                    resource = asyncPainterResource(imageUrl),
                                    contentDescription = "Image de ${poke.name?.fr}",
                                    modifier = Modifier.size(200.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            poke.sprites?.shiny?.let { shinyUrl ->
                                Text(
                                    text = "Version Shiny:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                KamelImage(
                                    resource = asyncPainterResource(shinyUrl),
                                    contentDescription = "Image shiny de ${poke.name?.fr}",
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
