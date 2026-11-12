package com.cartoniq.stealthtext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Samurai Glass palette (dark indigo, never black)
val Moon = Color(0xFFF7F1E8)
val Ink = Moon
val Sub = Color(0xCCF7F1E8)
val Bg = Color(0xFF1C1F66)
val Gold = Color(0xFFF0C96B)
val Vermilion = Color(0xFFF0443F)
val Crimson = Color(0xFFC81E3A)
val Cyan = Gold
val Magenta = Vermilion
val Lavender = Color(0xFFCDB8FF)
val Sun = Color(0xFFFFD98A)
val Warn = Color(0xFFFF9C8F)
val Good = Color(0xFF8DF0C4)
val InkDark = Color(0xFF1C1F66)

/** Night sky with a vermilion sun, ink-wash mountains and sakura petals. */
fun Modifier.prismBackground(): Modifier = this.drawBehind {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1C1F66), Color(0xFF4B2270), Color(0xFF8A2F5F))))
    val c = Offset(size.width * 0.75f, size.height * 0.2f)
    val glow = size.width * 0.55f
    drawCircle(Brush.radialGradient(listOf(Color(0xE6FF6B5E), Color.Transparent), center = c, radius = glow), radius = glow, center = c)
    drawCircle(Vermilion.copy(alpha = 0.92f), radius = size.width * 0.16f, center = c)
    fun ridge(base: Float, ys: FloatArray, color: Color) {
        val p = Path()
        p.moveTo(0f, size.height)
        val n = ys.size
        for (i in 0 until n) p.lineTo(size.width * i / (n - 1), size.height * (base - ys[i]))
        p.lineTo(size.width, size.height)
        p.close()
        drawPath(p, color)
    }
    ridge(0.80f, floatArrayOf(0f, 0.07f, 0.03f, 0.09f, 0.02f, 0.06f, 0f), Color(0x8C3A2378))
    ridge(0.86f, floatArrayOf(0.02f, 0f, 0.06f, 0.01f, 0.05f, 0f, 0.04f), Color(0xB32A2380))
    ridge(0.92f, floatArrayOf(0f, 0.04f, 0f, 0.05f, 0.01f, 0.04f, 0f), Color(0xE61E2068))
    val petal = Color(0xFFFFB7C5).copy(alpha = 0.7f)
    val spots = floatArrayOf(0.1f, 0.14f, 0.3f, 0.07f, 0.85f, 0.4f, 0.15f, 0.55f, 0.9f, 0.65f, 0.5f, 0.3f)
    var i = 0
    while (i < spots.size) {
        drawOval(petal, topLeft = Offset(size.width * spots[i], size.height * spots[i + 1]), size = Size(14.dp.toPx(), 8.dp.toPx()))
        i += 2
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.07f))))
            .border(1.5.dp, Color.White.copy(alpha = 0.38f), shape)
    ) {
        content()
    }
}

@Composable
fun GradientButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(29.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Vermilion, Crimson)))
            .border(1.5.dp, Gold.copy(alpha = 0.85f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Moon, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    }
}
