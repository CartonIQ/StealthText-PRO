package com.cartoniq.stealthtext

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import android.content.Intent
import android.os.Build
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Vermilion, Crimson)))
                        .border(1.5.dp, Gold, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("影", color = Moon, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif) }
                Spacer(Modifier.width(12.dp))
                Text("StealthText", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif)
            }
            GlassCard {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Gold))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Offline and private"), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(tr("Your Secrets.\nYour Control."), color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 33.sp, fontFamily = FontFamily.Serif)
                Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(Gold, Moon, Color.Transparent))))
                Text(tr("Hide encrypted messages, files and voice inside photos."), color = Sub, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            FeatureCard(Modifier.weight(1f).fillMaxHeight(), tr("Encode Text"), tr("Hide a message"), "隠") { onOpen(Screen.HideText) }
            Spacer(Modifier.width(14.dp))
            FeatureCard(Modifier.weight(1f).fillMaxHeight(), tr("Decode"), tr("Reveal anything"), "開") { onOpen(Screen.Reveal) }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            FeatureCard(Modifier.weight(1f).fillMaxHeight(), tr("Hide Files"), tr("PDF or TXT"), "巻") { onOpen(Screen.HideFile) }
            Spacer(Modifier.width(14.dp))
            FeatureCard(Modifier.weight(1f).fillMaxHeight(), tr("Voice Studio"), tr("Record and hide"), "声") { onOpen(Screen.HideVoice) }
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(tr("Tip"), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Send the saved PNG as a file or document. Sending it as a photo makes WhatsApp or Instagram compress it, and the secret is lost. Storm Mode is the exception for short texts.",
                    color = Ink, fontSize = 14.sp, lineHeight = 20.sp
                )
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
fun ToolsScreen(onOpen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Spy Tools"), null)
        FeatureCard(Modifier.fillMaxWidth(), tr("Split Secret"), tr("Hide one secret across two images"), "分") { onOpen(Screen.Split) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Shamir Split"), tr("Hide a secret in 3 to 5 images, any chosen number opens it"), "刃") { onOpen(Screen.Shamir) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Invisible Text"), tr("Hide a message inside normal-looking text"), "幻") { onOpen(Screen.InvisibleText) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Storm Mode"), tr("Survives JPEG compression. Short text only."), "嵐") { onOpen(Screen.Storm) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Hanko"), tr("Your signing seal and trusted contacts"), "判") { onOpen(Screen.Hanko) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Cover Scout"), "Rank photos by how well they hide secrets", "選") { onOpen(Screen.Scout) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Clean Photo"), "Remove location and camera data", "浄") { onOpen(Screen.Clean) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Scan Image"), tr("Check if a photo may hide data"), "眼") { onOpen(Screen.Scan) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Stealth Watermark"), tr("Hide a copyright tag in a photo"), "印") { onOpen(Screen.HideMark) }
        Spacer(Modifier.height(12.dp))
        FeatureCard(Modifier.fillMaxWidth(), tr("Check a Watermark"), tr("Reveal a tag, message, file or voice note"), "検") { onOpen(Screen.Reveal) }
    }
}

@Composable
fun VaultScreen(onReveal: (Uri) -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()
    var unlocked by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(ImageIO.vaultFiles(ctx)) }
    val scope = rememberCoroutineScope()
    val unlock: () -> Unit = {
        if (activity != null) {
            Auth.authenticate(activity, tr("Open private vault")) { ok, err ->
                unlocked = ok
                msg = err
            }
        } else {
            msg = "Cannot ask for authentication here."
        }
    }
    LaunchedEffect(Unit) { unlock() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Private Vault"), null)
        if (!unlocked) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(tr("The vault is locked."), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (msg.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(msg, color = Warn, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton(tr("Unlock with fingerprint or screen lock")) { unlock() }
                }
            }
        } else if (files.isEmpty()) {
            Text("Nothing here yet. Turn on \"Private vault only\" when hiding something.", color = Sub, fontSize = 15.sp)
        } else {
            files.forEach { f ->
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Image ${f.name.removePrefix("v_").removeSuffix(".png").takeLast(6)}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${f.length() / 1024} KB", color = Sub, fontSize = 12.sp)
                        Row {
                            TextButton(onClick = { onReveal(Uri.fromFile(f)) }) { Text(tr("Reveal"), color = Ink, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.Default) {
                                        val bmp = ImageIO.loadBitmap(ctx, Uri.fromFile(f), false)
                                        if (bmp == null) {
                                            false
                                        } else {
                                            val u = ImageIO.savePng(ctx, bmp)
                                            bmp.recycle()
                                            u != null
                                        }
                                    }
                                    msg = if (ok) "Exported to Pictures/StealthText." else "Export failed."
                                }
                            }) { Text(tr("Export"), color = Ink, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { shareFiles(ctx, listOf(Uri.fromFile(f)), true) }) { Text(tr("Share"), color = Ink, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = {
                                f.delete()
                                files = ImageIO.vaultFiles(ctx)
                            }) { Text(tr("Delete"), color = Warn, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
            if (msg.isNotEmpty()) Text(msg, color = Good, fontSize = 13.sp)
        }
    }
}

private fun isPin(s: String): Boolean = s.length in 4..8 && s.all { it.isDigit() }

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var hasPin by remember { mutableStateOf(Prefs.hasPin(ctx)) }
    var curPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinMsg by remember { mutableStateOf("") }
    var shake by remember { mutableStateOf(Prefs.shake(ctx)) }
    var limit by remember { mutableFloatStateOf(Prefs.limit(ctx).toFloat()) }
    var disguise by remember { mutableStateOf(Prefs.disguise(ctx)) }
    var showWipe by remember { mutableStateOf(false) }
    var doc by remember { mutableStateOf<Int?>(null) }
    var defBits by remember { mutableIntStateOf(Prefs.defBits(ctx) - 1) }
    var defVault by remember { mutableStateOf(Prefs.defVault(ctx)) }
    var defSmart by remember { mutableStateOf(Prefs.defSmart(ctx)) }
    var lockIdx by remember { mutableIntStateOf(Prefs.LOCK_OPTIONS.indexOf(Prefs.lockMs(ctx)).coerceAtLeast(1)) }
    var clipIdx by remember { mutableIntStateOf(Prefs.CLIP_OPTIONS.indexOf(Prefs.clipSec(ctx)).coerceAtLeast(1)) }
    var voiceIdx by remember { mutableIntStateOf(Prefs.VOICE_OPTIONS.indexOf(Prefs.voiceMax(ctx)).coerceAtLeast(0)) }
    var scaleIdx by remember { mutableIntStateOf(Prefs.SCALE_OPTIONS.indexOfFirst { Math.abs(it - Prefs.textScale(ctx)) < 0.001f }.coerceAtLeast(1)) }
    var secureOn by remember { mutableStateOf(Prefs.secure(ctx)) }
    var panicClose by remember { mutableStateOf(Prefs.panicClose(ctx)) }
    var showPw by remember { mutableStateOf(Prefs.showPw(ctx)) }
    var deadIdx by remember { mutableIntStateOf(Prefs.DEAD_OPTIONS.indexOf(Prefs.deadDays(ctx)).coerceAtLeast(0)) }
    var duressOn by remember { mutableStateOf(Prefs.duressSet(ctx)) }
    var duressPin by remember { mutableStateOf("") }
    var duressMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        ScreenHeader(tr("Settings"), null)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(tr("Language"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Lang.codes.indices.chunked(3).forEach { group ->
                    Row(Modifier.fillMaxWidth()) {
                        group.forEach { i ->
                            val code = Lang.codes[i]
                            TextButton(
                                onClick = {
                                    AppState.lang = code
                                    Prefs.putLang(ctx, code)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    Lang.names[i],
                                    color = if (AppState.lang == code) Ink else Sub,
                                    fontSize = 14.sp,
                                    fontWeight = if (AppState.lang == code) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        }
                        repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(tr("App PIN"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(tr("4 to 8 digits. Wrong PIN 3 times shows a fake crash screen."), color = Sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                if (hasPin) {
                    PasswordField(tr("Current PIN"), curPin, true) { curPin = it }
                    Spacer(Modifier.height(8.dp))
                }
                PasswordField(tr("New PIN"), newPin, true) { newPin = it }
                Spacer(Modifier.height(8.dp))
                GradientButton(if (hasPin) tr("Change PIN") else tr("Set PIN")) {
                    if (hasPin && !Prefs.verifyPin(ctx, curPin)) {
                        pinMsg = tr("Current PIN is wrong.")
                    } else if (!isPin(newPin)) {
                        pinMsg = tr("PIN must be 4 to 8 digits.")
                    } else {
                        Prefs.savePin(ctx, newPin)
                        Prefs.putFails(ctx, 0)
                        hasPin = true
                        curPin = ""
                        newPin = ""
                        pinMsg = tr("PIN saved.")
                    }
                }
                if (hasPin) {
                    TextButton(onClick = {
                        if (Prefs.verifyPin(ctx, curPin)) {
                            Prefs.clearPin(ctx)
                            Prefs.clearDuress(ctx)
                            duressOn = false
                            Prefs.putDisguise(ctx, false)
                            Disguise.apply(ctx, false)
                            disguise = false
                            hasPin = false
                            curPin = ""
                            pinMsg = tr("PIN removed.")
                        } else {
                            pinMsg = "Enter your current PIN to remove it."
                        }
                    }) { Text(tr("Remove PIN"), color = Warn, fontWeight = FontWeight.Bold) }
                }
                if (pinMsg.isNotEmpty()) Text(pinMsg, color = Ink, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (hasPin) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Duress PIN"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(tr("Enter it on the lock screen to silently wipe the vault and open an empty app."), color = Sub, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    PasswordField(tr("Duress PIN"), duressPin, true) { duressPin = it }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(tr("Save duress PIN")) {
                        if (!isPin(duressPin)) {
                            duressMsg = tr("PIN must be 4 to 8 digits.")
                        } else if (Prefs.verifyPin(ctx, duressPin)) {
                            duressMsg = tr("Duress PIN must differ from your app PIN.")
                        } else {
                            Prefs.saveDuress(ctx, duressPin)
                            duressOn = true
                            duressPin = ""
                            duressMsg = tr("Duress PIN saved.")
                        }
                    }
                    if (duressOn) {
                        TextButton(onClick = {
                            Prefs.clearDuress(ctx)
                            duressOn = false
                            duressMsg = ""
                        }) { Text(tr("Remove duress PIN"), color = Warn, fontWeight = FontWeight.Bold) }
                    }
                    if (duressMsg.isNotEmpty()) Text(duressMsg, color = Ink, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SwitchRow(tr("Shake to lock"), "Shake the phone while the app is open to lock it at once", shake) {
                    shake = it
                    Prefs.putShake(ctx, it)
                    (ctx.findActivity() as? MainActivity)?.refreshShake()
                }
                SwitchRow(tr("Disguise as Calculator"), "Launcher icon becomes a calculator. Type your PIN and press = to open.", disguise, hasPin) {
                    disguise = it
                    Prefs.putDisguise(ctx, it)
                    Disguise.apply(ctx, it)
                }
                Spacer(Modifier.height(6.dp))
                Text("Self-destruct after ${limit.roundToInt()} wrong PINs", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Wipes the vault, settings and PIN. Needs an app PIN.", color = Sub, fontSize = 12.sp)
                Slider(
                    value = limit,
                    onValueChange = { limit = it },
                    onValueChangeFinished = { Prefs.putLimit(ctx, limit.roundToInt()) },
                    valueRange = 3f..10f,
                    steps = 6
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(tr("Preferences"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                ChoiceRow(tr("Default bit depth"), listOf("1", "2", "3"), defBits) {
                    defBits = it
                    Prefs.putDefBits(ctx, it + 1)
                }
                SwitchRow(tr("Smart hiding by default"), tr("Start new hides with Smart hiding on"), defSmart) {
                    defSmart = it
                    Prefs.putDefSmart(ctx, it)
                }
                SwitchRow(tr("Private vault by default"), tr("Save new results in the vault, not the gallery"), defVault) {
                    defVault = it
                    Prefs.putDefVault(ctx, it)
                }
                ChoiceRow(tr("Auto-lock after leaving the app"), listOf("Now", "30 s", "1 min", "5 min"), lockIdx) {
                    lockIdx = it
                    Prefs.putLockMs(ctx, Prefs.LOCK_OPTIONS[it])
                }
                ChoiceRow(tr("Clear clipboard after"), listOf("15 s", "30 s", "60 s"), clipIdx) {
                    clipIdx = it
                    Prefs.putClipSec(ctx, Prefs.CLIP_OPTIONS[it])
                }
                ChoiceRow(tr("Longest voice note"), listOf("30 s", "60 s", "120 s"), voiceIdx) {
                    voiceIdx = it
                    Prefs.putVoiceMax(ctx, Prefs.VOICE_OPTIONS[it])
                }
                ChoiceRow(tr("Text size"), listOf("S", "M", "L", "XL"), scaleIdx) {
                    scaleIdx = it
                    Prefs.putTextScale(ctx, Prefs.SCALE_OPTIONS[it])
                    AppState.textScale = Prefs.SCALE_OPTIONS[it]
                }
                SwitchRow(tr("Screenshot protection"), tr("Blocks screenshots and hides the app in recent apps"), secureOn) {
                    secureOn = it
                    Prefs.putSecure(ctx, it)
                    (ctx.findActivity() as? MainActivity)?.applySecure()
                }
                SwitchRow(tr("Close the app on panic"), tr("The LOCK button and shake also close the app"), panicClose) {
                    panicClose = it
                    Prefs.putPanicClose(ctx, it)
                }
                ChoiceRow(tr("Dead-man switch: wipe if unused for"), listOf("Off", "7 d", "30 d", "90 d"), deadIdx) {
                    deadIdx = it
                    Prefs.putDeadDays(ctx, Prefs.DEAD_OPTIONS[it])
                    Prefs.putLastOpen(ctx, System.currentTimeMillis())
                }
                Text(
                    "Checked when the app is opened. If it was unused for longer than this, the vault, PIN and settings are wiped. Changing the phone clock can bypass it.",
                    color = Sub, fontSize = 12.sp, lineHeight = 17.sp
                )
                SwitchRow(tr("Show passwords while typing"), tr("Off is safer in public"), showPw) {
                    showPw = it
                    Prefs.putShowPw(ctx, it)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                SettingsRow(tr("About this app")) { doc = 0 }
                SettingsRow(tr("Privacy Policy")) { openLink(ctx, Intent.ACTION_VIEW, PRIVACY_URL) }
                SettingsRow(tr("Terms & Conditions")) { doc = 1 }
                SettingsRow(tr("Licenses & Copyright")) { doc = 2 }
                SettingsRow(tr("Data collection information")) { doc = 3 }
                SettingsRow(tr("Delete my data")) { showWipe = true }
                SettingsRow(tr("Contact Support")) { openLink(ctx, Intent.ACTION_SENDTO, "mailto:$SUPPORT_EMAIL?subject=StealthText%20support") }
                SettingsRow(tr("Rate this app")) { openPlay(ctx) }
                SettingsRow(tr("Share this app")) { shareApp(ctx) }
                SettingsRow(tr("Check for Updates")) { openPlay(ctx) }
                SettingsRow(tr("Report a Bug")) { reportBug(ctx) }
                if (Ads.privacyOptionsRequired(ctx)) {
                    SettingsRow(tr("Ad privacy options")) { ctx.findActivity()?.let { a -> Ads.showPrivacyOptions(a) } }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    val d = doc
    if (d != null) {
        AlertDialog(
            onDismissRequest = { doc = null },
            title = { Text(docTitle(d)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(docBody(d, appVersion(ctx)), fontSize = 14.sp, lineHeight = 20.sp) } },
            confirmButton = { TextButton(onClick = { doc = null }) { Text(tr("Close"), color = Ink, fontWeight = FontWeight.Bold) } }
        )
    }
    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            title = { Text(tr("Delete all app data?")) },
            text = { Text("This removes the vault, PIN and settings. PNGs already saved in your gallery are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    showWipe = false
                    Prefs.wipeAll(ctx)
                    Disguise.apply(ctx, false)
                    AppState.locked = false
                    AppState.onboarded = false
                }) { Text(tr("Delete"), color = Warn, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showWipe = false }) { Text(tr("Cancel"), color = Ink) } }
        )
    }
}

fun openLink(ctx: android.content.Context, action: String, uri: String) {
    try {
        ctx.startActivity(Intent(action, Uri.parse(uri)))
    } catch (e: Exception) {
    }
}

const val PRIVACY_URL = "https://cartoniq.github.io/privacy/"
const val SUPPORT_EMAIL = "cartoniq.business@gmail.com"
private const val PLAY_PACKAGE = "com.cartoniq.stealthtext"
private const val PLAY_WEB = "https://play.google.com/store/apps/details?id=" + PLAY_PACKAGE

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("›", color = Sub, fontSize = 18.sp)
    }
}

private fun appVersion(ctx: android.content.Context): String =
    try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "2.0.0"
    } catch (e: Exception) {
        "2.0.0"
    }

private fun openPlay(ctx: android.content.Context) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PLAY_PACKAGE")))
    } catch (e: Exception) {
        openLink(ctx, Intent.ACTION_VIEW, PLAY_WEB)
    }
}

private fun shareApp(ctx: android.content.Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "StealthText: hide secrets inside photos, 100% offline. $PLAY_WEB")
    }
    try {
        ctx.startActivity(Intent.createChooser(send, "StealthText"))
    } catch (e: Exception) {
    }
}

private fun reportBug(ctx: android.content.Context) {
    val body = "Describe the problem:\n\n\n---\nApp: StealthText ${appVersion(ctx)}\n" +
        "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
        "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
        "Language: ${AppState.lang}"
    val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "StealthText bug report")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        ctx.startActivity(mail)
    } catch (e: Exception) {
    }
}

private fun docTitle(i: Int): String = when (i) {
    0 -> tr("About this app")
    1 -> tr("Terms & Conditions")
    2 -> tr("Licenses & Copyright")
    else -> tr("Data collection information")
}

private fun docBody(i: Int, version: String): String = when (i) {
    0 -> "StealthText\n" + tr("Version") + " " + version + "\nCartonIQ\n\n" +
        "Hides encrypted text, files and voice notes inside photos. All hiding, locking and revealing works fully offline. The app has no accounts and no trackers of its own. It shows Google AdMob ads, only while your phone is online.\n\n" +
        "Hidden data is lost if an image is compressed or edited. A lost password, PIN or key file cannot be recovered."
    1 -> "By using StealthText you agree to these terms.\n\n" +
        "1. Use. For lawful, personal privacy. You are responsible for what you hide and share.\n\n" +
        "2. No warranty. The app is provided \"as is\". Hidden data can be lost if images are compressed, edited or deleted. Lost passwords, PINs and key files cannot be recovered.\n\n" +
        "3. Not a guarantee. Hiding data in images lowers its visibility but does not stop expert analysis. Do not rely on it as the only protection for life-critical secrets.\n\n" +
        "4. Ads. The app shows ads from Google AdMob while you are online. Ad providers have their own terms and privacy practices.\n\n" +
        "5. Liability. To the extent allowed by law, CartonIQ is not liable for data loss or damage caused by using the app.\n\n" +
        "6. Changes. These terms may change in future versions.\n\nContact: $SUPPORT_EMAIL"
    2 -> "StealthText \u00A9 2026 CartonIQ. All rights reserved.\n\n" +
        "This app includes open-source software licensed under the Apache License, Version 2.0:\n" +
        "- AndroidX and Jetpack Compose\n- Kotlin and kotlinx.coroutines\n\n" +
        "You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0\n\n" +
        "Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.\n\n" +
        "Google Mobile Ads SDK and Google User Messaging Platform are provided by Google under their own terms."
    else -> "Your secrets stay on your phone.\n\n" +
        "- Photos, files, messages, voice notes, passwords, PINs and key files are processed on your device and are never uploaded. There are no accounts.\n" +
        "- Your PIN is stored only as a salted hash. Fingerprint checks are done by Android.\n" +
        "- The microphone is used only while you record a voice note; the audio stays on your device.\n" +
        "- Ads: the app uses Google AdMob. When your phone is online, Google may collect device identifiers (such as the advertising ID), approximate location, IP address and ad interaction data to show and measure ads, as described in Google's policies. Where required, the app asks for your consent first and you can change it under Ad privacy options in Settings.\n" +
        "- Ads load only while online. Offline, no ad code runs and every feature still works.\n" +
        "- Delete My Data removes the vault, PIN and settings. PNG images already saved to your gallery are yours to delete."
}

@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Segmented(options, selected, onSelect)
    }
}
