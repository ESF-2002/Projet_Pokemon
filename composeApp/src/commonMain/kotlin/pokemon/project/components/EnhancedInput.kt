package pokemon.project.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedInput(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isVerifying: Boolean,
    enabled: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, animationSpec = tween(120))
    val pillElevation by animateDpAsState(targetValue = if (focused) 8.dp else 2.dp, animationSpec = tween(220))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = pillElevation
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BasicTextField(
                value = answer,
                onValueChange = onAnswerChange,
                singleLine = true,
                enabled = enabled && !isVerifying,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (answer.isEmpty() && !focused) {
                            Text(text = "Entrez votre réponse...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                        }
                        inner()
                    }
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            val canClick = enabled && answer.isNotBlank() && !isVerifying

            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .scale(pressScale)
                    .semantics { contentDescription = "verify_icon" },
                shape = CircleShape,
                color = if (canClick) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                tonalElevation = if (canClick) 6.dp else 0.dp
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = canClick) {
                        if (canClick) {
                            scope.launch {
                                pressed = true
                                onSubmit()
                                try { keyboardController?.hide() } catch (_: Exception) {}
                                delay(140)
                                pressed = false
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                    if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("✓", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
