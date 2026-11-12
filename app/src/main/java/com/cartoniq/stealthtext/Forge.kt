package com.cartoniq.stealthtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom

/** Password Forge: pronounceable random passwords and a rough strength estimate. */
object Forge {
    private const val CONS = "bdfgklmnprstvz"
    private const val VOW = "aeiou"
    private val common = listOf("password", "qwerty", "123456", "111111", "iloveyou", "admin", "letmein", "abc123", "welcome", "secret", "000000")

    /** Ten random syllables plus two digits, for example kamu-rebo-tipa-sudi-nelo-47 (about 68 bits). */
    fun generate(): String {
        val r = SecureRandom()
        val sb = StringBuilder()
        for (g in 0 until 5) {
            if (g > 0) sb.append('-')
            for (s in 0 until 2) {
                sb.append(CONS[r.nextInt(CONS.length)])
                sb.append(VOW[r.nextInt(VOW.length)])
            }
        }
        sb.append('-')
        sb.append(r.nextInt(10))
        sb.append(r.nextInt(10))
        return sb.toString()
    }

    /** Level 0 weak, 1 fair, 2 strong, 3 excellent, plus rough bits. An estimate, not a guarantee. */
    fun strength(pw: String): Pair<Int, Int> {
        if (pw.isEmpty()) return Pair(0, 0)
        var pool = 0
        if (pw.any { it.isLowerCase() }) pool += 26
        if (pw.any { it.isUpperCase() }) pool += 26
        if (pw.any { it.isDigit() }) pool += 10
        if (pw.any { !it.isLetterOrDigit() }) pool += 33
        val distinct = pw.toSet().size
        val effLen = distinct + (pw.length - distinct) * 0.25
        var bits = effLen * (Math.log(maxOf(pool, 2).toDouble()) / Math.log(2.0))
        val low = pw.lowercase()
        if (common.any { low.contains(it) }) bits = minOf(bits, 20.0)
        if (isSequence(low)) bits = minOf(bits, 25.0)
        val b = bits.toInt()
        val level = if (b < 40) 0 else if (b < 60) 1 else if (b < 80) 2 else 3
        return Pair(level, b)
    }

    private fun isSequence(s: String): Boolean {
        if (s.length < 6) return false
        var inc = true
        var dec = true
        for (i in 1 until s.length) {
            val d = s[i] - s[i - 1]
            if (d != 1) inc = false
            if (d != -1) dec = false
        }
        return inc || dec
    }
}

@Composable
fun PasswordForge(password: String, onGenerated: (String) -> Unit) {
    val ctx = LocalContext.current
    var forged by remember { mutableStateOf("") }
    val st = Forge.strength(password)
    val names = listOf("Weak", "Fair", "Strong", "Excellent")
    val colors = listOf(Warn, Sun, Good, Good)
    val fraction = if (password.isEmpty()) 0f else (st.first + 1) / 4f
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(CircleShape).background(colors[st.first]))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (password.isEmpty()) "Strength (rough estimate)" else names[st.first] + " (about " + st.second + " bits, rough)",
                color = Sub, fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                val g = Forge.generate()
                forged = g
                onGenerated(g)
            }) { Text(tr("Forge password"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
        if (forged.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(forged, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { copySecret(ctx, forged) }) { Text(tr("Copy"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            Text("Save it somewhere safe. A lost password cannot be recovered.", color = Sub, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}
