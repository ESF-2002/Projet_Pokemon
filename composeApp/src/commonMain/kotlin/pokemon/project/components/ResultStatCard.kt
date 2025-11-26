package pokemon.project.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ResultStatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071022)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = label, fontSize = MaterialTheme.typography.bodySmall.fontSize, color = Color(0xFF94A3B8))
                Text(text = value, fontSize = MaterialTheme.typography.bodyLarge.fontSize, fontWeight = MaterialTheme.typography.bodyLarge.fontWeight ?: androidx.compose.ui.text.font.FontWeight.ExtraBold, color = Color.White)
            }
            Box(modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text(text = label.first().toString(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = accent)
            }
        }
    }
}

