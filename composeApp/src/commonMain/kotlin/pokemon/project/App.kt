package pokemon.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    var gameState by remember { mutableStateOf(GameState.MENU) }
    var currentPokemon by remember { mutableStateOf<Pokemon?>(null) }
    var userAnswer by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var totalQuestions by remember { mutableIntStateOf(0) }
    var maxQuestions by remember { mutableIntStateOf(10) }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var isLoading by remember { mutableStateOf(false) }
    var correctAnswers by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Timer effect
    LaunchedEffect(gameState, timeLeft) {
        if (gameState == GameState.PLAYING && timeLeft > 0 && !showFeedback) {
            delay(1000)
            timeLeft--
        } else if (gameState == GameState.PLAYING && timeLeft == 0 && !showFeedback) {
            showFeedback = true
            isCorrect = false
            streak = 0
            totalQuestions++

            delay(2500)

            if (totalQuestions >= maxQuestions) {
                gameState = GameState.RESULTS
            } else {
                showFeedback = false
                loadNextPokemon(difficulty) { pokemon ->
                    currentPokemon = pokemon
                    userAnswer = ""
                    timeLeft = 30
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE63946),
            secondary = Color(0xFFF1FAEE),
            tertiary = Color(0xFF457B9D),
            background = Color(0xFF1D3557),
            surface = Color(0xFF2A4A6F),
            onPrimary = Color.White,
            onSecondary = Color(0xFF1D3557),
            onBackground = Color(0xFFF1FAEE),
            onSurface = Color(0xFFF1FAEE)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (gameState) {
                GameState.MENU -> MenuScreen(
                    onStartGame = { selectedDifficulty, selectedMaxQuestions ->
                        difficulty = selectedDifficulty
                        maxQuestions = selectedMaxQuestions
                        isLoading = true
                        gameState = GameState.PLAYING
                        score = 0
                        streak = 0
                        totalQuestions = 0
                        correctAnswers = 0
                        loadNextPokemon(selectedDifficulty) { pokemon ->
                            currentPokemon = pokemon
                            timeLeft = 30
                            isLoading = false
                        }
                    }
                )
                GameState.PLAYING -> QuizScreen(
                    pokemon = currentPokemon,
                    userAnswer = userAnswer,
                    onAnswerChange = { userAnswer = it },
                    onSubmit = {
                        val correctName = when (difficulty) {
                            Difficulty.EASY -> currentPokemon?.name?.fr?.lowercase()
                            Difficulty.NORMAL -> currentPokemon?.name?.fr?.lowercase()
                            Difficulty.HARD -> currentPokemon?.name?.en?.lowercase()
                        }

                        isCorrect = userAnswer.trim().lowercase() == correctName
                        showFeedback = true

                        if (isCorrect) {
                            score += (10 + streak * 2 + timeLeft)
                            streak++
                            correctAnswers++
                        } else {
                            streak = 0
                        }
                        totalQuestions++

                        scope.launch {
                            delay(2500)

                            if (totalQuestions >= maxQuestions) {
                                gameState = GameState.RESULTS
                            } else {
                                showFeedback = false
                                loadNextPokemon(difficulty) { pokemon ->
                                    currentPokemon = pokemon
                                    userAnswer = ""
                                    timeLeft = 30
                                }
                            }
                        }
                    },
                    score = score,
                    scope = scope,
                    streak = streak,
                    timeLeft = timeLeft,
                    showFeedback = showFeedback,
                    isCorrect = isCorrect,
                    difficulty = difficulty,
                    isLoading = isLoading,
                    currentQuestion = totalQuestions + 1,
                    maxQuestions = maxQuestions,
                    onQuit = {
                        gameState = GameState.RESULTS
                    }
                )
                GameState.RESULTS -> ResultsScreen(
                    score = score,
                    totalQuestions = totalQuestions,
                    correctAnswers = correctAnswers,
                    onPlayAgain = { gameState = GameState.MENU }
                )
            }
        }
    }
}

@Composable
fun MenuScreen(onStartGame: (Difficulty, Int) -> Unit) {
    var selectedDifficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var selectedQuestions by remember { mutableIntStateOf(10) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "🎮 POKÉMON QUIZ",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Devinez le nom du Pokémon!",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nombre de questions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(5, 10, 15, 20).forEach { count ->
                        FilterChip(
                            selected = selectedQuestions == count,
                            onClick = { selectedQuestions = count },
                            label = {
                                Text(
                                    text = count.toString(),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Choisissez la difficulté",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        DifficultyButton(
            text = "🟢 FACILE",
            description = "Noms en français, 30 secondes",
            isSelected = selectedDifficulty == Difficulty.EASY,
            onClick = { selectedDifficulty = Difficulty.EASY }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DifficultyButton(
            text = "🟡 NORMAL",
            description = "Noms en français, 30 secondes",
            isSelected = selectedDifficulty == Difficulty.NORMAL,
            onClick = { selectedDifficulty = Difficulty.NORMAL }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DifficultyButton(
            text = "🔴 DIFFICILE",
            description = "Noms en anglais, 30 secondes",
            isSelected = selectedDifficulty == Difficulty.HARD,
            onClick = { selectedDifficulty = Difficulty.HARD }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onStartGame(selectedDifficulty, selectedQuestions) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "COMMENCER",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DifficultyButton(
    text: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun QuizScreen(
    pokemon: Pokemon?,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    score: Int,
    streak: Int,
    timeLeft: Int,
    showFeedback: Boolean,
    isCorrect: Boolean,
    difficulty: Difficulty,
    scope: CoroutineScope,
    isLoading: Boolean,
    currentQuestion: Int,
    maxQuestions: Int,
    onQuit: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (showFeedback) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Question",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$currentQuestion/$maxQuestions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Score",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = score.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (streak > 0)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Série",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "🔥 $streak",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (streak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    timeLeft <= 5 -> Color(0xFFE63946).copy(alpha = 0.3f)
                    timeLeft <= 10 -> Color(0xFFF77F00).copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ ",
                    fontSize = 24.sp
                )
                Text(
                    text = "${timeLeft}s",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        timeLeft <= 5 -> Color(0xFFE63946)
                        timeLeft <= 10 -> Color(0xFFF77F00)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Pokemon image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        showFeedback && isCorrect -> Color(0xFF06D6A0).copy(alpha = 0.2f)
                        showFeedback && !isCorrect -> Color(0xFFE63946).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Qui est ce Pokémon?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    pokemon?.sprites?.regular?.let { imageUrl ->
                        KamelImage(
                            resource = asyncPainterResource(imageUrl),
                            contentDescription = "Pokemon à deviner",
                            modifier = Modifier.size(250.dp)
                        )
                    }

                    if (showFeedback) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isCorrect) "✅ CORRECT!" else "❌ FAUX!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Color(0xFF06D6A0) else Color(0xFFE63946)
                        )

                        if (!isCorrect) {
                            Text(
                                text = "C'était: ${
                                    when (difficulty) {
                                        Difficulty.EASY, Difficulty.NORMAL -> pokemon?.name?.fr
                                        Difficulty.HARD -> pokemon?.name?.en
                                    }
                                }",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (isCorrect) {
                            val points = 10 + (streak - 1) * 2 + timeLeft
                            Text(
                                text = "+$points points",
                                fontSize = 16.sp,
                                color = Color(0xFF06D6A0),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!showFeedback) {
                // Answer input
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = onAnswerChange,
                    label = {
                        Text(
                            when (difficulty) {
                                Difficulty.EASY, Difficulty.NORMAL -> "Nom en français"
                                Difficulty.HARD -> "Nom en anglais"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = userAnswer.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✓ VÉRIFIER LA RÉPONSE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onQuit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Terminer le quiz")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ResultsScreen(
    score: Int,
    totalQuestions: Int,
    correctAnswers: Int,
    onPlayAgain: () -> Unit
) {
    val accuracy = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions) * 100).toInt() else 0
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "🏆 RÉSULTATS",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Score Final",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = score.toString(),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Questions", totalQuestions.toString())
                    StatItem("Correctes", correctAnswers.toString())
                    StatItem("Précision", "$accuracy%")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        accuracy >= 90 -> "🌟 Excellent! Maître Pokémon!"
                        accuracy >= 70 -> "👍 Très bien! Continue comme ça!"
                        accuracy >= 50 -> "💪 Pas mal! Tu progresses!"
                        else -> "📚 Continue à t'entraîner!"
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "REJOUER",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

enum class GameState {
    MENU, PLAYING, RESULTS
}

enum class Difficulty {
    EASY, NORMAL, HARD
}

fun loadNextPokemon(difficulty: Difficulty, onLoaded: (Pokemon?) -> Unit) {
    kotlinx.coroutines.GlobalScope.launch {
        val pokemon = Greeting().fetchPokemon()
        onLoaded(pokemon)
    }
}
