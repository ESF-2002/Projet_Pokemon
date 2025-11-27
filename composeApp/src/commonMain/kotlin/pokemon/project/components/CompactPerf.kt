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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment

@Composable
fun CompactPerf(label: String, percent: Int, accent: Color, modifier: Modifier = Modifier) {
    val p = percent.coerceIn(0, 100)
    Column(modifier = modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF071022))) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, fontSize = 13.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "${p}%", fontSize = 18.sp, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { p / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)), color = accent, trackColor = Color(0x332C3A52))
            }
        }
    }
}
