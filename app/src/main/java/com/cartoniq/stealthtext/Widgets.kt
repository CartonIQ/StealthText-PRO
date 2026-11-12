package com.cartoniq.stealthtext

import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.border
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Cover(val uri: Uri, val preview: ImageBitmap?, val w: Int, val h: Int)

suspend fun loadCover(ctx: Context, uri: Uri): Cover = withContext(Dispatchers.Default) {
    val size = ImageIO.imageSize(ctx, uri)
    val p = ImageIO.loadPreview(ctx, uri)
    Cover(uri, p?.asImageBitmap(), size?.first ?: 0, size?.second ?: 0)
}

@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) onPicked(u)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun copySecret(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("secret", text)
    clip.description.setExtras(PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) })
    cm.setPrimaryClip(clip)
    Handler(Looper.getMainLooper()).postDelayed({ cm.clearPrimaryClip() }, Prefs.clipSec(ctx) * 1000L)
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?) {
    Column {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text(tr("‹ Back"), color = Sub, fontSize = 15.sp) }
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun PasswordField(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (Prefs.showPw(LocalContext.current)) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SwitchRow(label: String, sub: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Sub, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = InkDark,
                checkedTrackColor = Gold,
                checkedBorderColor = Gold,
                uncheckedThumbColor = Moon.copy(alpha = 0.85f),
                uncheckedTrackColor = Color(0x33141755),
                uncheckedBorderColor = Gold.copy(alpha = 0.45f)
            )
        )
    }
}

@Composable
fun FeatureCard(modifier: Modifier, title: String, sub: String, kanji: String, onClick: () -> Unit) {
    GlassCard(modifier) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 124.dp)
                .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(kanji, color = Gold, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(10.dp))
            Column {
                Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text(sub, color = Sub, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ScanLine() {
    val tr = rememberInfiniteTransition(label = "scan")
    val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "y")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.horizontalGradient(listOf(Cyan, Magenta)),
            topLeft = Offset(0f, size.height * t - 3.dp.toPx()),
            size = Size(size.width, 6.dp.toPx())
        )
    }
}

@Composable
fun PreviewBox(preview: ImageBitmap?, scanning: Boolean, hint: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().height(170.dp)) {
        Box(Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
            if (preview != null) {
                Image(bitmap = preview, contentDescription = "Selected photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(hint, color = Sub, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(16.dp))
            }
            if (scanning) ScanLine()
        }
    }
}

@Composable
fun CapacityMeter(fraction: Float, over: Boolean) {
    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0xFFE4E8F2))) {
        Box(
            Modifier.fillMaxWidth(fraction).height(10.dp).clip(CircleShape)
                .background(if (over) Brush.horizontalGradient(listOf(Warn, Warn)) else Brush.horizontalGradient(listOf(Cyan, Magenta)))
        )
    }
}

@Composable
fun KeyGrid(rows: List<List<String>>, onKey: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { k ->
                    if (k.isEmpty()) {
                        Spacer(Modifier.weight(1f).height(60.dp))
                    } else {
                        Box(
                            Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
                                .clickable(role = Role.Button) { onKey(k) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PanicButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Vermilion, Crimson)))
            .border(1.5.dp, Gold, CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("守", color = Moon, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif)
    }
}

/** Refined segmented control: one dark glass bar with a thin gold edge and a gold selected segment. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Color(0x33141755))
            .border(1.dp, Gold.copy(alpha = 0.55f), shape).padding(3.dp)
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                    .background(
                        if (sel) Brush.verticalGradient(listOf(Color(0xFFF7DA8A), Gold))
                        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    o,
                    color = if (sel) InkDark else Sub,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
