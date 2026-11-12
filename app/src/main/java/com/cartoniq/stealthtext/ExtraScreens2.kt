package com.cartoniq.stealthtext

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoutItem(val uri: Uri, val preview: ImageBitmap, val busy: Double, val capKb: Int, val score: Double)

/** Cover Scout: ranks several photos by how well they hide data (busy texture and size). */
@Composable
fun ScoutScreen(onBack: () -> Unit, onUse: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ScoutItem>>(emptyList()) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                working = true
                status = ""
                val res = withContext(Dispatchers.Default) {
                    runCatching {
                        val out = ArrayList<ScoutItem>()
                        for (u in uris) {
                            val size = ImageIO.imageSize(ctx, u) ?: continue
                            val pv = ImageIO.loadPreview(ctx, u, 640) ?: continue
                            val ratio = Stego.busyRatio(pv)
                            val cap1 = Stego.capacity(size.first, size.second, 1, 1)
                            val score = ratio * Math.sqrt(maxOf(cap1, 1).toDouble())
                            out.add(ScoutItem(u, pv.asImageBitmap(), ratio, cap1 / 1024, score))
                        }
                        out.sortedByDescending { it.score }
                    }
                }
                working = false
                res.onSuccess {
                    items = it
                    if (it.isEmpty()) status = "Could not read these photos."
                }.onFailure { status = it.message ?: tr("Something went wrong.") }
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Cover Scout"), onBack)
        Text(
            "Pick up to 6 photos. Scout ranks them: busy, textured photos with many pixels hide secrets best and are harder to detect.",
            color = Sub, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(12.dp))
        GradientButton(if (working) tr("Scanning...") else tr("Choose photos (up to 6)"), !working) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(status, color = Warn, fontSize = 14.sp)
        }
        items.forEachIndexed { i, it ->
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(bitmap = it.preview, contentDescription = "Photo ${i + 1}", contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (i == 0) "#1 Best pick" else "#" + (i + 1), color = if (i == 0) Gold else Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        val grade = if (it.busy >= 0.5) "Excellent texture" else if (it.busy >= 0.25) "Good texture" else "Plain, hides worse"
                        Text(grade + " (" + (it.busy * 100).toInt() + "% busy)", color = Ink, fontSize = 13.sp)
                        Text("Holds about " + it.capKb + " KB at 1 bit", color = Sub, fontSize = 12.sp)
                        TextButton(onClick = {
                            AppState.pendingCover = it.uri
                            onUse()
                        }) { Text(tr("Use for hiding"), color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Clean Photo: shows what hidden metadata a photo carries and saves a copy without it. */
@Composable
fun CleanScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var cover by remember { mutableStateOf<Cover?>(null) }
    var report by remember { mutableStateOf<List<String>>(emptyList()) }
    var format by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var statusOk by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberImagePicker { u ->
        scope.launch {
            cover = loadCover(ctx, u)
            saved = null
            status = ""
            report = withContext(Dispatchers.Default) { ImageIO.exifReport(ctx, u) }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Clean Photo"), onBack)
        Text(
            "Photos often carry your location, camera and time. Clean Photo saves a copy with all of that removed.",
            color = Sub, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(12.dp))
        PreviewBox(cover?.preview, busy, tr("Tap to choose a photo to check")) { pick() }
        if (report.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Found in this photo", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    for (line in report) {
                        Text(line, color = if (line.contains("FOUND")) Warn else Ink, fontSize = 14.sp, fontWeight = if (line.contains("FOUND")) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Save as", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Segmented(listOf("JPEG", "PNG"), format) { format = it }
        Spacer(Modifier.height(14.dp))
        GradientButton(if (busy) tr("Working...") else tr("Save clean copy"), cover != null && !busy) {
            val c = cover
            val png = format == 1
            if (c != null) {
                scope.launch {
                    busy = true
                    status = ""
                    saved = null
                    val res = withContext(Dispatchers.Default) {
                        runCatching {
                            val bmp = ImageIO.loadBitmap(ctx, c.uri, true) ?: throw StegoException("Could not open this photo.")
                            try {
                                if (png) {
                                    ImageIO.savePng(ctx, bmp, "_clean") ?: throw StegoException("Could not save the image.")
                                } else {
                                    val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                                    Canvas(flat).apply {
                                        drawColor(AColor.WHITE)
                                        drawBitmap(bmp, 0f, 0f, null)
                                    }
                                    try {
                                        ImageIO.saveJpeg(ctx, flat) ?: throw StegoException("Could not save the image.")
                                    } finally {
                                        flat.recycle()
                                    }
                                }
                            } finally {
                                bmp.recycle()
                            }
                        }
                    }
                    busy = false
                    res.onSuccess {
                        saved = it
                        statusOk = true
                        status = "Clean copy saved to Pictures/StealthText. Location, camera and time are removed."
                    }.onFailure {
                        statusOk = false
                        status = it.message ?: tr("Something went wrong.")
                    }
                }
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(status, color = if (statusOk) Good else Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        val su = saved
        if (su != null) {
            Spacer(Modifier.height(12.dp))
            ShareActions(listOf(su), if (format == 1) "image/png" else "image/jpeg")
        }
        Spacer(Modifier.height(24.dp))
    }
}
