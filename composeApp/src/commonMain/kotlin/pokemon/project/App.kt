package pokemon.project

import androidx.compose.animation.core.*
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
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
    // Empêche les doubles résolutions (timeout + submit simultanés)
    var questionResolved by remember { mutableStateOf(false) }

    // Timer effect
    LaunchedEffect(gameState, timeLeft) {
        if (gameState == GameState.PLAYING && timeLeft > 0 && !showFeedback) {
            delay(1000)
            timeLeft--
        } else if (gameState == GameState.PLAYING && timeLeft == 0 && !showFeedback) {
            // éviter double résolution
            if (!questionResolved) {
                questionResolved = true
                showFeedback = true
                isCorrect = false
                streak = 0
                totalQuestions++

                delay(2500)

                if (totalQuestions >= maxQuestions) {
                    gameState = GameState.RESULTS
                } else {
                    showFeedback = false
                    // préparer la prochaine question
                    loadNextPokemon(scope) { pokemon ->
                        currentPokemon = pokemon
                        userAnswer = ""
                        timeLeft = 30
                        questionResolved = false
                    }
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
                        // réinitialiser les flags visuels/état
                        showFeedback = false
                        isCorrect = false
                        userAnswer = ""
                        questionResolved = false
                        loadNextPokemon(scope) { pokemon ->
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
                        // éviter double résolution si timeout et submit arrivent presque en même temps
                        if (questionResolved) return@QuizScreen
                        questionResolved = true

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
                                loadNextPokemon(scope) { pokemon ->
                                    currentPokemon = pokemon
                                    userAnswer = ""
                                    timeLeft = 30
                                    questionResolved = false
                                }
                            }
                        }
                    },
                    score = score,
                    streak = streak,
                    timeLeft = timeLeft,
                    showFeedback = showFeedback,
                    isCorrect = isCorrect,
                    difficulty = difficulty,
                    isLoading = isLoading,
                    // Afficher le numéro de la question en tenant compte si on est en phase de feedback
                    currentQuestion = if (showFeedback) totalQuestions else totalQuestions + 1,
                    maxQuestions = maxQuestions,
                    onQuit = {
                        // permettre de revenir au menu depuis le quiz
                        gameState = GameState.MENU
                    }
                )
                GameState.RESULTS -> ResultsScreen(
                    score = score,
                    totalQuestions = totalQuestions,
                    correctAnswers = correctAnswers,
                    onPlayAgain = {
                        // relancer la partie avec la même config
                        score = 0
                        streak = 0
                        totalQuestions = 0
                        correctAnswers = 0
                        showFeedback = false
                        isCorrect = false
                        userAnswer = ""
                        questionResolved = false
                        isLoading = true
                        gameState = GameState.PLAYING
                        loadNextPokemon(scope) { pokemon ->
                            currentPokemon = pokemon
                            timeLeft = 30
                            isLoading = false
                        }
                    },
                    onHome = {
                        // revenir simplement au menu pour pouvoir changer la config
                        gameState = GameState.MENU
                    }
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
    // gradient moderne
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .semantics { contentDescription = "menu_screen" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Pokémon Quiz",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Devinez le Pokémon avant la fin du temps",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x22344556))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Nombre de questions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 10, 15, 20).forEach { count ->
                            Box(modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .clickable { selectedQuestions = count }
                                .background(
                                    color = if (selectedQuestions == count) Color(0xFFE63946) else Color(0xFF0B1220),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = count.toString(), color = if (selectedQuestions == count) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Choisissez la difficulté", fontSize = 16.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(Difficulty.EASY to "FACILE", Difficulty.NORMAL to "NORMAL", Difficulty.HARD to "DIFFICILE").forEach { (d, label) ->
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .clickable { selectedDifficulty = d }
                        .background(
                            color = if (selectedDifficulty == d) Color(0xFFE63946) else Color(0xFF0B1220),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = if (selectedDifficulty == d) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onStartGame(selectedDifficulty, selectedQuestions) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "start_button" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE63946)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "COMMENCER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
            .semantics { contentDescription = "quiz_screen" }
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
                modifier = Modifier.semantics { contentDescription = "question_counter" },
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
                modifier = Modifier.semantics { contentDescription = "score_display" },
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
                modifier = Modifier.semantics { contentDescription = "streak_display" },
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "timer_display" },
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .semantics { contentDescription = "pokemon_card" },
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
                            contentDescription = "pokemon_image",
                            modifier = Modifier.size(250.dp)
                        )
                    }

                    if (showFeedback) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isCorrect) "✅ CORRECT!" else "❌ FAUX!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Color(0xFF06D6A0) else Color(0xFFE63946),
                            modifier = Modifier.semantics { contentDescription = "feedback_message" }
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
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { contentDescription = "correct_answer_display" }
                            )
                        }

                        if (isCorrect) {
                            val points = 10 + (streak - 1) * 2 + timeLeft
                            Text(
                                text = "+$points points",
                                fontSize = 16.sp,
                                color = Color(0xFF06D6A0),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { contentDescription = "points_earned" }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!showFeedback) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "answer_input" },
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
                        .height(56.dp)
                        .semantics { contentDescription = "submit_button" },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "quit_button" },
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
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val accuracy = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions) * 100).toInt() else 0
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "results_screen" }
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Résultats",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFE63946)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Score Final", fontSize = 14.sp, color = Color(0xFF94A3B8))
                Text(text = score.toString(), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.semantics { contentDescription = "final_score" })

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Questions", totalQuestions.toString(), "total_questions")
                    StatItem("Correctes", correctAnswers.toString(), "correct_answers")
                    StatItem("Précision", "$accuracy%", "accuracy")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        accuracy >= 90 -> "🌟 Excellent! Maître Pokémon!"
                        accuracy >= 70 -> "👍 Très bien! Continue comme ça!"
                        accuracy >= 50 -> "💪 Pas mal! Tu progresses!"
                        else -> "📚 Continue à t'entraîner!"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = "performance_message" }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("REJOUER", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.weight(1f).height(56.dp),
                border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ACCUEIL", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatItem(label: String, value: String, contentDesc: String = "") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { contentDescription = contentDesc }
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

fun loadNextPokemon(scope: CoroutineScope, onLoaded: (Pokemon?) -> Unit) {
    scope.launch {
        val pokemon = Greeting().fetchPokemon()
        onLoaded(pokemon)
    }
}
