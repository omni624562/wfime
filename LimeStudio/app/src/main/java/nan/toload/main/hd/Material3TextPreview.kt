package nan.toload.main.hd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MockKey(label: String, fontSize: Int, isLarge: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 46.dp)
            .background(color = Color(0xFF444746), shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = if (isLarge) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
fun CompareTextSize() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("左：目前大小 (18dp) | 右：調整後目標 (25dp)", color = Color.White, fontSize = 12.sp)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 目前感覺
            MockKey("q", 18, false)
            
            // 您的期望方向 (Gboard 風格)
            MockKey("q", 25, true)
        }
    }
}
