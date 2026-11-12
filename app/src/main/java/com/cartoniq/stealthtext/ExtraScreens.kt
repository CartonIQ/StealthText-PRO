package com.cartoniq.stealthtext

import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun KeyFilePicker(keyName: String, onPicked: (String, ByteArray?) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            scope.launch {
                val r = withContext(Dispatchers.IO) { ImageIO.readFile(ctx, u) }
                if (r != null) onPicked(r.first, r.second)
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tr("Key file (optional)"), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(if (keyName.isEmpty()) tr("Password plus a file you choose") else keyName, color = Sub, fontSize = 12.sp)
        }
        TextButton(onClick = { pick.launch(arrayOf("*/*")) }) {
            Text(if (keyName.isEmpty()) tr("Choose") else tr("Change"), color = Ink, fontWeight = FontWeight.Bold)
        }
        if (keyName.isNotEmpty()) {
            TextButton(onClick = { onPicked("", null) }) { Text(tr("Remove"), color = Warn, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun InvisibleTextScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reveal by remember { mutableStateOf(false) }
    var cover by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var pasted by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var keyName by remember { mutableStateOf("") }
    var keyBytes by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<String?>(null) }

    val ready = password.length >= Stego.MIN_PASSWORD && !busy &&
        (if (reveal) pasted.isNotEmpty() else cover.isNotEmpty() && secret.isNotEmpty())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Invisible Text"), onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { reveal = false; output = null; status = "" }) {
                Text(tr("Hide"), color = if (!reveal) Ink else Sub, fontWeight = if (!reveal) FontWeight.ExtraBold else FontWeight.Normal)
            }
            TextButton(onClick = { reveal = true; output = null; status = "" }) {
                Text(tr("Reveal"), color = if (reveal) Ink else Sub, fontWeight = if (reveal) FontWeight.ExtraBold else FontWeight.Normal)
            }
        }
        Text(
            if (reveal) "Paste the text you received."
            else "Your secret is locked and mixed into normal-looking text with invisible characters. Paste it in any chat. Some apps remove invisible characters, so test first.",
            color = Sub, fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        if (reveal) {
            OutlinedTextField(
                value = pasted,
                onValueChange = { pasted = it },
                label = { Text(tr("Pasted text")) },
                minLines = 4,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = cover,
                onValueChange = { cover = it },
                label = { Text(tr("Normal-looking text")) },
                minLines = 2,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(tr("Secret message")) },
                minLines = 3,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        PasswordField(tr("Password (6+ characters)"), password) { password = it }
        KeyFilePicker(keyName) { n, b ->
            keyName = n
            keyBytes = b
        }
        Spacer(Modifier.height(12.dp))
        GradientButton(if (busy) tr("Working...") else if (reveal) tr("Reveal text") else tr("Hide in text"), ready) {
            val pw = Crypto.mixKey(password, keyBytes)
            val cv = cover
            val sc = secret
            val pt = pasted
            val rv = reveal
            scope.launch {
                busy = true
                status = ""
                output = null
                val res = withContext(Dispatchers.Default) {
                    runCatching { if (rv) ZeroWidth.reveal(pt, pw) else ZeroWidth.hide(cv, sc, pw) }
                }
                busy = false
                res.onSuccess { output = it; ctx.findActivity()?.let { a -> Ads.maybeShow(a) } }.onFailure { status = it.message ?: tr("Something went wrong.") }
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(status, color = Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        val out = output
        if (out != null) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (reveal) tr("Secret message") else "Your text (looks normal)", color = Sub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(out, color = Ink, fontSize = 17.sp, lineHeight = 25.sp)
                    Spacer(Modifier.height(12.dp))
                    GradientButton(tr("Copy (clipboard clears in 30 s)")) { copySecret(ctx, out) }
                    if (!reveal) {
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, out)
                            }
                            ctx.startActivity(Intent.createChooser(send, tr("Share text")))
                        }) { Text(tr("Share text"), color = Ink, fontWeight = FontWeight.Bold) }
                    }
                    TextButton(onClick = { output = null }) { Text(tr("Hide result"), color = Sub, fontSize = 15.sp) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ShamirScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var join by remember { mutableStateOf(false) }
    val covers = remember { mutableStateListOf<Cover>() }
    var secret by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var keyName by remember { mutableStateOf("") }
    var keyBytes by remember { mutableStateOf<ByteArray?>(null) }
    var kf by remember { mutableFloatStateOf(2f) }
    var bits by remember { mutableFloatStateOf(1f) }
    var vaultOnly by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var statusOk by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var savedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val pick = rememberImagePicker { u ->
        scope.launch {
            val c = loadCover(ctx, u)
            if (covers.size < 5) covers.add(c)
            status = ""
        }
    }

    val n = covers.size
    val depth = bits.roundToInt()
    val threshold = kf.roundToInt().coerceIn(2, maxOf(2, n))
    val size = secret.toByteArray(Charsets.UTF_8).size
    var minCap = Int.MAX_VALUE
    for (c in covers) minCap = minOf(minCap, Stego.capacity(c.w, c.h, depth, false))
    val tooBig = !join && n >= 3 && size + 50 > minCap
    val ready = password.length >= Stego.MIN_PASSWORD && !busy &&
        (if (join) n >= 2 else n >= 3 && size > 0 && !tooBig)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Shamir Split"), onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { join = false; result = null; status = "" }) {
                Text(tr("Split"), color = if (!join) Ink else Sub, fontWeight = if (!join) FontWeight.ExtraBold else FontWeight.Normal)
            }
            TextButton(onClick = { join = true; result = null; status = "" }) {
                Text(tr("Join"), color = if (join) Ink else Sub, fontWeight = if (join) FontWeight.ExtraBold else FontWeight.Normal)
            }
        }
        Text(
            if (join) "Add the images you have (any order), then enter the password."
            else "Add 3 to 5 photos. Any chosen number of them can rebuild the secret, fewer cannot.",
            color = Sub, fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            for (c in covers) {
                val p = c.preview
                if (p != null) {
                    Image(bitmap = p, contentDescription = "Chosen photo", contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
                }
            }
        }
        Row {
            TextButton(onClick = { pick() }, enabled = n < 5) { Text("Add image ($n/5)", color = Ink, fontWeight = FontWeight.Bold) }
            if (n > 0) TextButton(onClick = { covers.clear(); result = null }) { Text(tr("Clear"), color = Warn, fontWeight = FontWeight.Bold) }
        }
        if (!join) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(tr("Secret message")) },
                minLines = 3,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (n >= 3) {
                Spacer(Modifier.height(8.dp))
                Text("Images needed to open it: $threshold of $n", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Slider(value = kf.coerceIn(2f, n.toFloat()), onValueChange = { kf = it }, valueRange = 2f..n.toFloat(), steps = n - 3)
            }
        }
        Spacer(Modifier.height(8.dp))
        PasswordField(tr("Password (6+ characters)"), password) { password = it }
        KeyFilePicker(keyName) { nm, b ->
            keyName = nm
            keyBytes = b
        }
        if (!join) {
            Text("Bits per colour: $depth", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(value = bits, onValueChange = { bits = it }, valueRange = 1f..3f, steps = 1)
            if (tooBig) Text("Too big for these photos (limit ${minCap / 1024} KB).", color = Warn, fontSize = 12.sp)
            SwitchRow(tr("Private vault only"), "Keep all images out of the gallery.", vaultOnly) { vaultOnly = it }
        }
        Spacer(Modifier.height(12.dp))
        GradientButton(if (busy) tr("Working...") else if (join) tr("Join and reveal") else "Split into $n images", ready) {
            val list = covers.toList()
            val pw = Crypto.mixKey(password, keyBytes)
            val text = secret
            val vOnly = vaultOnly
            val th = threshold
            val d = depth
            scope.launch {
                busy = true
                status = ""
                result = null
                savedUris = emptyList()
                if (join) {
                    val res = withContext(Dispatchers.Default) {
                        runCatching {
                            val shares = ArrayList<ByteArray>()
                            for ((i, c) in list.withIndex()) {
                                val bmp = ImageIO.loadBitmap(ctx, c.uri, false) ?: throw StegoException("Could not open image ${i + 1}.")
                                shares.add(try { Stego.decode(bmp, pw) } finally { bmp.recycle() })
                            }
                            String(Payload.unpack(Shamir.combine(shares)).data, Charsets.UTF_8)
                        }
                    }
                    busy = false
                    res.onSuccess { result = it; ctx.findActivity()?.let { a -> Ads.maybeShow(a) } }.onFailure {
                        statusOk = false
                        status = it.message ?: tr("Something went wrong.")
                    }
                } else {
                    val res = withContext(Dispatchers.Default) {
                        runCatching {
                            val payload = Payload.pack(Payload.TEXT, "", text.toByteArray(Charsets.UTF_8))
                            val shares = Shamir.split(payload, th, list.size)
                            val outs = ArrayList<Uri>()
                            for ((i, c) in list.withIndex()) {
                                val bmp = ImageIO.loadBitmap(ctx, c.uri, true) ?: throw StegoException("Could not open image ${i + 1}.")
                                val out = try { Stego.encode(bmp, shares[i], pw, null, null, d) } finally { bmp.recycle() }
                                val uu = try {
                                    (if (vOnly) ImageIO.saveToVault(ctx, out, "_s${i + 1}of${list.size}") else ImageIO.savePng(ctx, out, "_s${i + 1}of${list.size}"))
                                        ?: throw StegoException("Could not save image ${i + 1}.")
                                } finally {
                                    out.recycle()
                                }
                                outs.add(uu)
                            }
                            outs.toList()
                        }
                    }
                    busy = false
                    res.onSuccess {
                        statusOk = true
                        savedUris = it
                        status = if (vOnly) "All parts saved in your private vault." else "All parts saved to Pictures/StealthText."
                        ctx.findActivity()?.let { a -> Ads.maybeShow(a) }
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
        if (savedUris.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ShareActions(savedUris)
        }
        val shown = result
        if (shown != null) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(tr("Secret message"), color = Sub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(shown, color = Ink, fontSize = 17.sp, lineHeight = 25.sp)
                    TextButton(onClick = { result = null }) { Text(tr("Hide result"), color = Sub, fontSize = 15.sp) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ScanScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var cover by remember { mutableStateOf<Cover?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Analyzer.Scan?>(null) }
    val pick = rememberImagePicker { u ->
        scope.launch {
            cover = loadCover(ctx, u)
            result = null
            status = ""
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Scan Image"), onBack)
        PreviewBox(cover?.preview, busy, tr("Tap to choose a photo to check")) { pick() }
        Spacer(Modifier.height(12.dp))
        GradientButton(if (busy) tr("Scanning...") else tr("Scan"), cover != null && !busy) {
            val c = cover
            if (c != null) {
                scope.launch {
                    busy = true
                    status = ""
                    result = null
                    val res = withContext(Dispatchers.Default) {
                        runCatching {
                            val bmp = ImageIO.loadBitmap(ctx, c.uri, false) ?: throw StegoException("Could not open this image.")
                            try {
                                Analyzer.scan(bmp)
                            } finally {
                                bmp.recycle()
                            }
                        }
                    }
                    busy = false
                    res.onSuccess { result = it; ctx.findActivity()?.let { a -> Ads.maybeShow(a) } }.onFailure { status = it.message ?: tr("Something went wrong.") }
                }
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(status, color = Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        val r = result
        if (r != null) {
            Spacer(Modifier.height(16.dp))
            val verdict = if (r.stealth) tr("StealthText data found") else if (r.p >= 0.8) tr("Suspicious") else if (r.p >= 0.3) tr("Unclear") else tr("Looks normal")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(verdict, color = if (r.stealth || r.p >= 0.8) Warn else Good, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("LSB pattern: " + (r.p * 100).roundToInt() + "%", color = Ink, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Estimate only. It reliably spots StealthText images, but other hiding tools or small messages can be missed.",
                        color = Sub, fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StormScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reveal by remember { mutableStateOf(false) }
    var cover by remember { mutableStateOf<Cover?>(null) }
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var statusOk by remember { mutableStateOf(false) }
    var test70 by remember { mutableStateOf<Boolean?>(null) }
    var test50 by remember { mutableStateOf<Boolean?>(null) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var output by remember { mutableStateOf<String?>(null) }
    val pick = rememberImagePicker { u ->
        scope.launch {
            cover = loadCover(ctx, u)
            status = ""
            output = null
            saved = null
            test70 = null
            test50 = null
        }
    }
    val c = cover
    val limit = if (c != null) Storm.maxPacked(c.w, c.h) else 0
    val size = message.toByteArray(Charsets.UTF_8).size
    val tooLong = !reveal && c != null && size + 12 > limit
    val ready = c != null && !busy &&
        (if (reveal) password.isNotEmpty() else password.length >= Stego.MIN_PASSWORD && size > 0 && !tooLong)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Storm Mode"), onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { reveal = false; output = null; status = "" }) {
                Text(tr("Hide"), color = if (!reveal) Ink else Sub, fontWeight = if (!reveal) FontWeight.ExtraBold else FontWeight.Normal)
            }
            TextButton(onClick = { reveal = true; output = null; status = "" }) {
                Text(tr("Reveal"), color = if (reveal) Ink else Sub, fontWeight = if (reveal) FontWeight.ExtraBold else FontWeight.Normal)
            }
        }
        Text(
            tr("Survives JPEG compression. Short text only.") + " " +
                if (reveal) "" else "The photo is scaled to at most 1280 px. Resizing afterwards (for example by Instagram) breaks it. Busy photos work best; faint blocks may show on flat areas. Beta.",
            color = Sub, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(12.dp))
        PreviewBox(c?.preview, busy, tr("Tap to choose a cover photo")) { pick() }
        Spacer(Modifier.height(12.dp))
        if (!reveal) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text(tr("Secret message")) },
                minLines = 2,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (c == null) "Choose a photo to see the limit." else "About $size of ${maxOf(0, limit - 12)} bytes used.",
                color = if (tooLong) Warn else Sub, fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }
        PasswordField(if (reveal) tr("Password") else tr("Password (6+ characters)"), password) { password = it }
        if (!reveal) PasswordForge(password) { password = it }
        Spacer(Modifier.height(12.dp))
        GradientButton(if (busy) tr("Working...") else if (reveal) tr("Reveal text") else tr("Hide and save PNG"), ready) {
            val cv = cover
            val msg = message
            val pw = password
            val rv = reveal
            if (cv != null) {
                scope.launch {
                    busy = true
                    status = ""
                    output = null
                    saved = null
                    test70 = null
                    test50 = null
                    if (rv) {
                        val res = withContext(Dispatchers.Default) {
                            runCatching {
                                val bmp = ImageIO.loadBitmap(ctx, cv.uri, false) ?: throw StegoException("Could not open this image.")
                                try {
                                    Storm.decode(bmp, pw)
                                } finally {
                                    bmp.recycle()
                                }
                            }
                        }
                        busy = false
                        res.onSuccess { output = it; ctx.findActivity()?.let { a -> Ads.maybeShow(a) } }.onFailure {
                            statusOk = false
                            status = it.message ?: tr("Something went wrong.")
                        }
                    } else {
                        val res = withContext(Dispatchers.Default) {
                            runCatching {
                                val bmp = ImageIO.loadBitmap(ctx, cv.uri, true) ?: throw StegoException("Could not open this photo.")
                                val out = try {
                                    Storm.encode(bmp, msg, pw)
                                } finally {
                                    bmp.recycle()
                                }
                                val t1 = Storm.jpegTest(out, pw, msg, 70)
                                val t2 = Storm.jpegTest(out, pw, msg, 50)
                                val u = try {
                                    ImageIO.savePng(ctx, out) ?: throw StegoException("Could not save the image.")
                                } finally {
                                    out.recycle()
                                }
                                Triple(u, t1, t2)
                            }
                        }
                        busy = false
                        res.onSuccess {
                            saved = it.first
                            test70 = it.second
                            test50 = it.third
                            statusOk = true
                            status = tr("Saved to Pictures/StealthText as PNG.")
                            ctx.findActivity()?.let { a -> Ads.maybeShow(a) }
                        }.onFailure {
                            statusOk = false
                            status = it.message ?: tr("Something went wrong.")
                        }
                    }
                }
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(status, color = if (statusOk) Good else Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        val t1 = test70
        val t2 = test50
        if (t1 != null && t2 != null) {
            Spacer(Modifier.height(8.dp))
            Text("Self-test, JPEG quality 70: " + (if (t1) "passed" else "failed"), color = if (t1) Good else Warn, fontSize = 13.sp)
            Text("Self-test, JPEG quality 50: " + (if (t2) "passed" else "failed"), color = if (t2) Good else Warn, fontSize = 13.sp)
            if (!t1) Text("Try a busier photo or a shorter message.", color = Sub, fontSize = 12.sp)
        }
        val su = saved
        if (su != null) {
            Spacer(Modifier.height(12.dp))
            ShareActions(listOf(su))
        }
        val out = output
        if (out != null) {
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(tr("Secret message"), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(out, color = Ink, fontSize = 17.sp, lineHeight = 25.sp)
                    Spacer(Modifier.height(12.dp))
                    GradientButton(tr("Copy (clipboard clears in 30 s)")) { copySecret(ctx, out) }
                    TextButton(onClick = { output = null }) { Text(tr("Hide result"), color = Sub, fontSize = 15.sp) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun HankoScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var myKey by remember { mutableStateOf<String?>(null) }
    var contacts by remember { mutableStateOf(Prefs.contacts(ctx)) }
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { myKey = withContext(Dispatchers.Default) { Hanko.publicKeyB64() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Hanko"), onBack)
        Text(
            "Your Hanko is a signing seal. Turn on \"Sign with my Hanko\" when hiding, and the receiver can check the message really came from you. Share your public key once, by text. The private key never leaves your phone.",
            color = Sub, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("My Hanko", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val mk = myKey
                if (mk == null) {
                    Text("Preparing...", color = Sub, fontSize = 14.sp)
                } else {
                    Text(Hanko.fingerprint(mk), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(mk, color = Sub, fontSize = 11.sp, maxLines = 3)
                    Row {
                        TextButton(onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("hanko", mk))
                            msg = "Public key copied."
                        }) { Text("Copy key", color = Ink, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, mk)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Hanko"))
                        }) { Text("Share key", color = Ink, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Trusted contacts", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (contacts.isEmpty()) Text("No contacts yet.", color = Sub, fontSize = 14.sp)
                for (c in contacts) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.first, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(Hanko.fingerprint(c.second), color = Sub, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            Prefs.removeContact(ctx, c.first)
                            contacts = Prefs.contacts(ctx)
                        }) { Text(tr("Remove"), color = Warn, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Their public key") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                GradientButton("Save contact") {
                    if (name.isBlank() || !Hanko.isValidKey(key)) {
                        msg = "Enter a name and a valid public key."
                    } else {
                        Prefs.addContact(ctx, name.trim(), key)
                        contacts = Prefs.contacts(ctx)
                        name = ""
                        key = ""
                        msg = "Contact saved."
                    }
                }
            }
        }
        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(msg, color = Ink, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}
