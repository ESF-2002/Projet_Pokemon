package pokemon.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CompactPerf(label: String, percent: Int, accent: Color, modifier: Modifier = Modifier) {
    val p = percent.coerceIn(0, 100)
    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF071022))) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(text = label, fontSize = MaterialTheme.typography.bodySmall.fontSize, color = Color(0xFF94A3B8))
                    Text(text = "${p}%", fontSize = MaterialTheme.typography.bodySmall.fontSize, color = Color.White, fontWeight = MaterialTheme.typography.bodySmall.fontWeight ?: androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { p / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)), color = accent, trackColor = Color(0x332C3A52))
            }
        }
    }
}

