package com.cartoniq.stealthtext

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shares one or more images straight from the app. As a file the image is sent untouched; as an image, chat apps may compress it. */
fun shareFiles(ctx: Context, uris: List<Uri>, asFile: Boolean, mime: String = "image/png") {
    if (uris.isEmpty()) return
    val list = ArrayList<Uri>()
    for (u in uris) list.add(ImageIO.shareUri(ctx, u))
    val intent = if (list.size == 1) {
        Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, list[0]) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply { putParcelableArrayListExtra(Intent.EXTRA_STREAM, list) }
    }
    intent.type = if (asFile) "application/octet-stream" else mime
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val clip = ClipData.newRawUri("image", list[0])
    for (i in 1 until list.size) clip.addItem(ClipData.Item(list[i]))
    intent.clipData = clip
    try {
        ctx.startActivity(Intent.createChooser(intent, "StealthText"))
    } catch (e: Exception) {
    }
}

@Composable
fun ShareActions(uris: List<Uri>, mime: String = "image/png") {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        GradientButton(tr("Share as file")) { shareFiles(ctx, uris, true, mime) }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { shareFiles(ctx, uris, false, mime) }, modifier = Modifier.weight(1f)) {
                Text(tr("Share as image"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            TextButton(
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { ImageIO.copyToDownloads(ctx, uris, mime) }
                        msg = if (ok) "Saved to Downloads/StealthText." else "Could not save."
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(tr("Save to Downloads"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
        Text(
            "Share as file keeps the hidden data safe. Chat apps compress images and destroy it.",
            color = Sub, fontSize = 12.sp, lineHeight = 17.sp
        )
        if (msg.isNotEmpty()) Text(msg, color = Good, fontSize = 13.sp)
    }
}
