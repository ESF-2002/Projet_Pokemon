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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager

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
                    currentQuestion = if (showFeedback) totalQuestions else totalQuestions + 1,
                    maxQuestions = maxQuestions
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
    maxQuestions: Int
) {
    val scrollState = rememberScrollState()
    val isWideScreenState = remember { mutableStateOf(false) }

    val totalTime = 30f
    val timerProgress = timeLeft / totalTime
    val progressQuiz = currentQuestion.toFloat() / maxQuestions.toFloat()
    val dynamicGradient = remember(showFeedback, isCorrect) {
        if (showFeedback && isCorrect) Brush.verticalGradient(listOf(Color(0xFF022C22), Color(0xFF064E3B)))
        else if (showFeedback && !isCorrect) Brush.verticalGradient(listOf(Color(0xFF3F0D13), Color(0xFF5A1E27)))
        else Brush.linearGradient(listOf(Color(0xFF0F1124), Color(0xFF151B37), Color(0xFF1E2750)))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(dynamicGradient)
                .semantics { contentDescription = "quiz_screen" }
                .padding(innerPadding)
                .padding(18.dp)
                .verticalScroll(scrollState)
        ) {
            val isWide = maxWidth > 720.dp
            LaunchedEffect(isWide) {
                isWideScreenState.value = isWide
            }

            val infinite = rememberInfiniteTransition()
            val timerPulse by infinite.animateFloat(
                initialValue = 1f,
                targetValue = if (timeLeft <= 10) 1.08f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = if (timeLeft <= 5) 400 else 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val localScope = rememberCoroutineScope()
            var isVerifying by remember { mutableStateOf(false) }


            val questionFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            var imageVisible by remember { mutableStateOf(false) }
            LaunchedEffect(pokemon?.sprites?.regular) {
                imageVisible = false
                if (pokemon?.sprites?.regular != null) {
                    delay(60)
                    imageVisible = true
                    delay(80)
                    try {
                        questionFocusRequester.requestFocus()
                    } catch (_: Exception) { }
                }
            }

            LaunchedEffect(currentQuestion) {
                delay(40)
                try {
                    focusManager.clearFocus(force = true)
                    questionFocusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) { }
            }

            LaunchedEffect(pokemon) {
                if (pokemon == null) return@LaunchedEffect
                delay(60)
                try {
                    focusManager.clearFocus(force = true)
                    questionFocusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) { }
            }

            val imageAlpha by animateFloatAsState(targetValue = if (imageVisible) 1f else 0f, animationSpec = tween(400))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 6.dp)
                }
                return@BoxWithConstraints
            }

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    // left / image column
                    Column(modifier = Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x661A2238)),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0x332F3D55))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.semantics { contentDescription = "question_counter" }) {
                                        Text("Question", fontSize = 12.sp, color = Color(0xFF8FA3C4))
                                        Text("$currentQuestion/$maxQuestions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDEE9F7))
                                    }
                                    Column(modifier = Modifier.semantics { contentDescription = "score_display" }, horizontalAlignment = Alignment.End) {
                                        Text("Score", fontSize = 12.sp, color = Color(0xFF8FA3C4))
                                        Text(score.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                LinearProgressIndicator(progress = { progressQuiz }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)), trackColor = Color(0x332C3A52), color = if (showFeedback && isCorrect) Color(0xFF06D6A0) else MaterialTheme.colorScheme.primary)

                                Spacer(Modifier.height(18.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202C44)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).padding(end = 10.dp).semantics { contentDescription = "streak_display" }) {
                                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Série", fontSize = 12.sp, color = Color(0xFF8FA3C4))
                                            Text("🔥 $streak", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (streak > 0) MaterialTheme.colorScheme.primary else Color(0xFFDEE9F7))
                                        }
                                    }

                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202C44)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).padding(start = 10.dp).semantics { contentDescription = "timer_display" }) {
                                        Box(Modifier.padding(14.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(timerPulse)) {
                                                CircularProgressIndicator(progress = { timerProgress }, modifier = Modifier.size(70.dp), strokeWidth = 8.dp, color = when {
                                                    timeLeft <= 5 -> Color(0xFFE63946)
                                                    timeLeft <= 10 -> Color(0xFFF77F00)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }, trackColor = Color(0x332C3A52))

                                                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                                    Text("${timeLeft}s", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = when {
                                                        timeLeft <= 5 -> Color(0xFFE63946)
                                                        timeLeft <= 10 -> Color(0xFFF77F00)
                                                        else -> Color(0xFFDEE9F7)
                                                    })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    showFeedback && isCorrect -> Color(0x3306D6A0)
                                    showFeedback && !isCorrect -> Color(0x33E63946)
                                    else -> Color(0x66202C44)
                                }
                            ),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0x2240577A))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Qui est ce Pokémon?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDEE9F7))
                                Spacer(modifier = Modifier.height(18.dp))

                                pokemon?.sprites?.regular?.let { imageUrl ->
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        val imageSize = minOf(maxWidth * 0.7f, 320.dp)
                                        KamelImage(
                                            resource = asyncPainterResource(imageUrl),
                                            contentDescription = "pokemon_image",
                                            modifier = Modifier
                                                .size(imageSize)
                                                .clip(RoundedCornerShape(18.dp))
                                                .alpha(imageAlpha)
                                        )
                                    }
                                }

                                if (showFeedback) {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(text = if (isCorrect) "✅ CORRECT!" else "❌ FAUX!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if (isCorrect) Color(0xFF06D6A0) else Color(0xFFE63946))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val keyboardController = LocalSoftwareKeyboardController.current
                        val inputEnabled = !showFeedback
                        key(currentQuestion) {
                            EnhancedInput(
                                 answer = userAnswer,
                                 onAnswerChange = onAnswerChange,
                                 onSubmit = {
                                    // animate verify
                                    isVerifying = true
                                    localScope.launch {
                                        delay(600)
                                        onSubmit()
                                        isVerifying = false
                                        keyboardController?.hide()
                                    }
                                },
                                isVerifying = isVerifying,
                                enabled = inputEnabled,
                                focusRequester = questionFocusRequester
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if (showFeedback) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (!isCorrect) {
                                    Text(text = "C'était: ${when (difficulty) { Difficulty.EASY, Difficulty.NORMAL -> pokemon?.name?.fr; Difficulty.HARD -> pokemon?.name?.en }}", color = Color(0xFFDEE9F7))
                                } else {
                                    val points = 10 + (streak - 1) * 2 + timeLeft
                                    Text(text = "+$points points", color = Color(0xFF06D6A0))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x661A2238)),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0x332F3D55))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.semantics { contentDescription = "question_counter" }) {
                                    Text("Question", fontSize = 12.sp, color = Color(0xFF8FA3C4))
                                    Text("$currentQuestion/$maxQuestions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDEE9F7))
                                }
                                Column(modifier = Modifier.semantics { contentDescription = "score_display" }, horizontalAlignment = Alignment.End) {
                                    Text("Score", fontSize = 12.sp, color = Color(0xFF8FA3C4))
                                    Text(score.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(progress = { progressQuiz }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)), trackColor = Color(0x332C3A52), color = if (showFeedback && isCorrect) Color(0xFF06D6A0) else MaterialTheme.colorScheme.primary)

                            Spacer(Modifier.height(18.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202C44)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).padding(end = 10.dp).semantics { contentDescription = "streak_display" }) {
                                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Série", fontSize = 12.sp, color = Color(0xFF8FA3C4))
                                        Text("🔥 $streak", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (streak > 0) MaterialTheme.colorScheme.primary else Color(0xFFDEE9F7))
                                    }
                                }

                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202C44)), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).padding(start = 10.dp).semantics { contentDescription = "timer_display" }) {
                                    Box(Modifier.padding(14.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(progress = { timerProgress }, modifier = Modifier.size(70.dp), strokeWidth = 8.dp, color = when {
                                                timeLeft <= 5 -> Color(0xFFE63946)
                                                timeLeft <= 10 -> Color(0xFFF77F00)
                                                else -> MaterialTheme.colorScheme.primary
                                            }, trackColor = Color(0x332C3A52))

                                            Text("${timeLeft}s", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = when {
                                                timeLeft <= 5 -> Color(0xFFE63946)
                                                timeLeft <= 10 -> Color(0xFFF77F00)
                                                else -> Color(0xFFDEE9F7)
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(if (showFeedback) 1.04f else 1f),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                showFeedback && isCorrect -> Color(0x3306D6A0)
                                showFeedback && !isCorrect -> Color(0x33E63946)
                                else -> Color(0x66202C44)
                            }
                        ),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, Color(0x2240577A))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Qui est ce Pokémon?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDEE9F7))
                            Spacer(modifier = Modifier.height(20.dp))

                            pokemon?.sprites?.regular?.let { imageUrl ->
                                KamelImage(
                                    resource = asyncPainterResource(imageUrl),
                                    contentDescription = "pokemon_image",
                                    modifier = Modifier.size(260.dp)
                                )
                            }

                            if (showFeedback) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(text = if (isCorrect) "✅ CORRECT!" else "❌ FAUX!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = if (isCorrect) Color(0xFF06D6A0) else Color(0xFFE63946))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val keyboardControllerMobile = LocalSoftwareKeyboardController.current
                    val inputEnabledMobile = !showFeedback

                    key(currentQuestion) {
                        EnhancedInput(
                            answer = userAnswer,
                            onAnswerChange = onAnswerChange,
                            onSubmit = {
                                isVerifying = true
                                localScope.launch {
                                    delay(800)
                                    onSubmit()
                                    isVerifying = false
                                    keyboardControllerMobile?.hide()
                                }
                            },
                            isVerifying = isVerifying,
                            enabled = inputEnabledMobile,
                            focusRequester = questionFocusRequester
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedInput(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isVerifying: Boolean,
    enabled: Boolean,
    focusRequester: FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        LaunchedEffect(focusRequester) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) { }
        }

        var boxFocused by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, if (boxFocused) Color(0x44E63946) else Color(0x2240577A))
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
            ) {
                val inputBrush = Brush.horizontalGradient(listOf(Color(0xFF202C44), Color(0xFF253548)))
                Box(modifier = Modifier
                    .matchParentSize()
                    .background(inputBrush)
                )

                Box(modifier = Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.02f), Color.Transparent)))
                )

                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier
                        .width(6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFE63946), Color(0xFFE06A5A))))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val canSubmit = enabled && answer.isNotBlank()
                    TextField(
                        value = answer,
                        onValueChange = onAnswerChange,
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { boxFocused = it.isFocused }
                            .padding(end = 0.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        placeholder = {
                            Text(text = "Entrez votre réponse...", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        },
                        singleLine = true,
                        trailingIcon = {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.6.dp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (canSubmit) Color(0xFFE63946) else Color(0xFF6A2630))
                                        .then(if (canSubmit) Modifier.clickable { onSubmit(); try { keyboardController?.hide() } catch (_: Exception) { } } else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = if (canSubmit) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                     )
                }
            }
        }
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
        val pokemon = try {
            Greeting().fetchPokemon()
        } catch (_: Exception) {
            null
        }
        onLoaded(pokemon)
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
