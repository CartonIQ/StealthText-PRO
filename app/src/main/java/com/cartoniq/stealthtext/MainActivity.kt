package com.cartoniq.stealthtext

import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

enum class Screen { Home, Vault, Tools, Settings, HideText, HideFile, HideVoice, HideMark, Reveal, Split, Shamir, InvisibleText, Scan, Hanko, Storm, Scout, Clean }

private val tabScreens = setOf(Screen.Home, Screen.Vault, Screen.Tools, Screen.Settings)

class MainActivity : ComponentActivity(), SensorEventListener {
    private var lastShake = 0L
    private var stoppedAt = 0L
    private var sensorManager: SensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecure()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (!AppState.started) {
            AppState.started = true
            AppState.lang = Prefs.lang(this)
            AppState.textScale = Prefs.textScale(this)
            AppState.onboarded = Prefs.onboarded(this)
            AppState.locked = Prefs.hasPin(this)
        }
        checkDeadMan()
        setContent { StealthApp(this) }
        Ads.start(this)
    }

    override fun onStart() {
        super.onStart()
        checkDeadMan()
        if (stoppedAt > 0L && Prefs.hasPin(this) && SystemClock.elapsedRealtime() - stoppedAt > Prefs.lockMs(this)) {
            AppState.locked = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (AppState.onboarded && !AppState.locked) Prefs.putLastOpen(this, System.currentTimeMillis())
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        refreshShake()
        Ads.start(this)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun checkDeadMan() {
        if (Prefs.deadManExpired(this)) {
            Prefs.wipeAll(this)
            Disguise.apply(this, false)
            AppState.onboarded = false
            AppState.locked = false
        }
    }

    fun applySecure() {
        if (Prefs.secure(this)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun refreshShake() {
        val sm = sensorManager ?: return
        sm.unregisterListener(this)
        if (!Prefs.shake(this)) return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        try {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        } catch (e: SecurityException) {
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g > 2.7f) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShake > 1500L) {
                lastShake = now
                panic()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    /** Locks the app at once. Without a PIN there is nothing to unlock with, so the app closes instead. */
    fun panic() {
        if (!AppState.onboarded) return
        if (Prefs.hasPin(this)) {
            AppState.locked = true
            if (Prefs.panicClose(this)) finishAndRemoveTask()
        } else {
            finishAndRemoveTask()
        }
    }
}

@Composable
fun StealthApp(activity: MainActivity) {
    var screen by remember { mutableStateOf(Screen.Home) }
    var back by remember { mutableStateOf(Screen.Home) }
    var revealUri by remember { mutableStateOf<Uri?>(null) }
    val open: (Screen, Screen) -> Unit = { s, b ->
        if (s == Screen.Reveal) revealUri = null
        screen = s
        back = b
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = InkDark,
            secondary = Vermilion,
            background = Bg,
            surface = Color(0xFF2A2380),
            onSurface = Moon,
            onBackground = Moon,
            outline = Color(0x99F7F1E8),
            outlineVariant = Color(0x55F7F1E8)
        )
    ) {
        val dir = if (Lang.isRtl(AppState.lang)) LayoutDirection.Rtl else LayoutDirection.Ltr
        val baseDensity = LocalDensity.current
        val scaled = Density(baseDensity.density, baseDensity.fontScale * AppState.textScale)
        CompositionLocalProvider(LocalLayoutDirection provides dir, LocalDensity provides scaled) {
        Box(Modifier.fillMaxSize().prismBackground().systemBarsPadding().imePadding()) {
            if (!AppState.onboarded) {
                OnboardingScreen(onDone = {
                    Prefs.putOnboarded(activity)
                    AppState.onboarded = true
                })
            } else if (AppState.crashed) {
                FakeCrashScreen(onClose = {
                    AppState.crashed = false
                    AppState.locked = true
                    activity.finishAffinity()
                })
            } else if (AppState.locked) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    if (Prefs.disguise(activity)) CalculatorScreen() else PinLockScreen()
                }
            } else {
                BackHandler(enabled = screen !in tabScreens) { screen = back }
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            Screen.Home -> HomeScreen { s -> open(s, Screen.Home) }
                            Screen.Vault -> VaultScreen { u ->
                                revealUri = u
                                screen = Screen.Reveal
                                back = Screen.Vault
                            }
                            Screen.Tools -> ToolsScreen { s -> open(s, Screen.Tools) }
                            Screen.Settings -> SettingsScreen()
                            Screen.HideText -> HideScreen(HideMode.Text) { screen = back }
                            Screen.HideFile -> HideScreen(HideMode.File) { screen = back }
                            Screen.HideVoice -> HideScreen(HideMode.Voice) { screen = back }
                            Screen.HideMark -> HideScreen(HideMode.Mark) { screen = back }
                            Screen.Reveal -> RevealScreen(revealUri) { screen = back }
                            Screen.Split -> SplitScreen { screen = back }
                            Screen.Shamir -> ShamirScreen { screen = back }
                            Screen.InvisibleText -> InvisibleTextScreen { screen = back }
                            Screen.Scan -> ScanScreen { screen = back }
                            Screen.Hanko -> HankoScreen { screen = back }
                            Screen.Storm -> StormScreen { screen = back }
                            Screen.Scout -> ScoutScreen(onBack = { screen = back }, onUse = {
                                screen = Screen.HideText
                                back = Screen.Tools
                            })
                            Screen.Clean -> CleanScreen { screen = back }
                        }
                        PanicButton(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp)) {
                            activity.panic()
                        }
                    }
                    if (screen == Screen.Home || screen == Screen.Vault || screen == Screen.Tools) {
                        AdBanner()
                    }
                    if (screen in tabScreens) {
                        BottomNav(screen) { t -> screen = t }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    val items = listOf(
        Triple(Screen.Home, "家", "Home"),
        Triple(Screen.Vault, "蔵", "Vault"),
        Triple(Screen.Tools, "具", "Tools"),
        Triple(Screen.Settings, "設", "Settings")
    )
    val shape = RoundedCornerShape(33.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))))
            .border(1.5.dp, Color.White.copy(alpha = 0.38f), shape)
            .padding(8.dp)
    ) {
        items.forEach { (s, kanji, label) ->
            val sel = s == current
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                    .background(if (sel) Gold.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(s) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(kanji, color = if (sel) Gold else Sub, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif)
                Text(tr(label), color = if (sel) Gold else Sub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
