package com.cartoniq.stealthtext

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Prefs {
    val LOCK_OPTIONS = longArrayOf(0L, 30_000L, 60_000L, 300_000L)
    val CLIP_OPTIONS = intArrayOf(15, 30, 60)
    val VOICE_OPTIONS = intArrayOf(30, 60, 120)
    val SCALE_OPTIONS = floatArrayOf(0.9f, 1.0f, 1.15f, 1.3f)
    val DEAD_OPTIONS = intArrayOf(0, 7, 30, 90)

    private fun sp(ctx: Context): SharedPreferences = ctx.getSharedPreferences("stealth_prefs", Context.MODE_PRIVATE)

    fun defBits(ctx: Context): Int = sp(ctx).getInt("def_bits", 1).coerceIn(1, 3)
    fun putDefBits(ctx: Context, v: Int) { sp(ctx).edit().putInt("def_bits", v).apply() }
    fun defVault(ctx: Context): Boolean = sp(ctx).getBoolean("def_vault", false)
    fun putDefVault(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("def_vault", v).apply() }
    fun defSmart(ctx: Context): Boolean = sp(ctx).getBoolean("def_smart", false)
    fun putDefSmart(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("def_smart", v).apply() }
    fun lockMs(ctx: Context): Long = sp(ctx).getLong("lock_ms", 30_000L)
    fun putLockMs(ctx: Context, v: Long) { sp(ctx).edit().putLong("lock_ms", v).apply() }
    fun clipSec(ctx: Context): Int = sp(ctx).getInt("clip_sec", 30)
    fun putClipSec(ctx: Context, v: Int) { sp(ctx).edit().putInt("clip_sec", v).apply() }
    fun secure(ctx: Context): Boolean = sp(ctx).getBoolean("secure", true)
    fun putSecure(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("secure", v).apply() }
    fun textScale(ctx: Context): Float = sp(ctx).getFloat("text_scale", 1.0f)
    fun putTextScale(ctx: Context, v: Float) { sp(ctx).edit().putFloat("text_scale", v).apply() }
    fun panicClose(ctx: Context): Boolean = sp(ctx).getBoolean("panic_close", false)
    fun putPanicClose(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("panic_close", v).apply() }
    fun showPw(ctx: Context): Boolean = sp(ctx).getBoolean("show_pw", false)
    fun putShowPw(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("show_pw", v).apply() }
    fun voiceMax(ctx: Context): Int = sp(ctx).getInt("voice_max", 120)
    fun putVoiceMax(ctx: Context, v: Int) { sp(ctx).edit().putInt("voice_max", v).apply() }
    fun deadDays(ctx: Context): Int = sp(ctx).getInt("dead_days", 0)
    fun putDeadDays(ctx: Context, v: Int) { sp(ctx).edit().putInt("dead_days", v).apply() }
    fun lastOpen(ctx: Context): Long = sp(ctx).getLong("last_open", 0L)
    fun putLastOpen(ctx: Context, v: Long) { sp(ctx).edit().putLong("last_open", v).apply() }

    /** Dead-man switch: true if the app was not used for longer than the chosen number of days. */
    fun deadManExpired(ctx: Context): Boolean {
        val d = deadDays(ctx)
        val last = lastOpen(ctx)
        if (d <= 0 || last == 0L) return false
        return System.currentTimeMillis() - last > d * 86_400_000L
    }

    fun lastAd(ctx: Context): Long = sp(ctx).getLong("last_ad", 0L)
    fun putLastAd(ctx: Context, v: Long) { sp(ctx).edit().putLong("last_ad", v).apply() }

    fun lang(ctx: Context): String = Lang.initial(sp(ctx).getString("lang", null))
    fun putLang(ctx: Context, v: String) { sp(ctx).edit().putString("lang", v).apply() }

    fun onboarded(ctx: Context): Boolean = sp(ctx).getBoolean("onboarded", false)
    fun putOnboarded(ctx: Context) { sp(ctx).edit().putBoolean("onboarded", true).apply() }

    fun hasPin(ctx: Context): Boolean = sp(ctx).getString("pin_hash", null) != null

    fun savePin(ctx: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp(ctx).edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(ctx: Context, pin: String): Boolean {
        val s = sp(ctx)
        val saltS = s.getString("pin_salt", null) ?: return false
        val hashS = s.getString("pin_hash", null) ?: return false
        if (pin.isEmpty()) return false
        val got = hashPin(pin, Base64.decode(saltS, Base64.NO_WRAP))
        return MessageDigest.isEqual(got, Base64.decode(hashS, Base64.NO_WRAP))
    }

    fun clearPin(ctx: Context) { sp(ctx).edit().remove("pin_salt").remove("pin_hash").apply() }

    fun shake(ctx: Context): Boolean = sp(ctx).getBoolean("shake", true)
    fun putShake(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("shake", v).apply() }

    fun limit(ctx: Context): Int = sp(ctx).getInt("limit", 5)
    fun putLimit(ctx: Context, v: Int) { sp(ctx).edit().putInt("limit", v).apply() }

    fun disguise(ctx: Context): Boolean = sp(ctx).getBoolean("disguise", false)
    fun putDisguise(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("disguise", v).apply() }

    fun fails(ctx: Context): Int = sp(ctx).getInt("fails", 0)
    fun putFails(ctx: Context, v: Int) { sp(ctx).edit().putInt("fails", v).apply() }

    fun duressSet(ctx: Context): Boolean = sp(ctx).getString("duress_hash", null) != null

    fun saveDuress(ctx: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp(ctx).edit()
            .putString("duress_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("duress_hash", Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP))
            .apply()
    }

    fun verifyDuress(ctx: Context, pin: String): Boolean {
        val s = sp(ctx)
        val saltS = s.getString("duress_salt", null) ?: return false
        val hashS = s.getString("duress_hash", null) ?: return false
        if (pin.isEmpty()) return false
        val got = hashPin(pin, Base64.decode(saltS, Base64.NO_WRAP))
        return MessageDigest.isEqual(got, Base64.decode(hashS, Base64.NO_WRAP))
    }

    fun clearDuress(ctx: Context) { sp(ctx).edit().remove("duress_salt").remove("duress_hash").apply() }

    fun isBurned(ctx: Context, id: String): Boolean = sp(ctx).getStringSet("burned", emptySet())?.contains(id) == true

    fun markBurned(ctx: Context, id: String) {
        val set = HashSet<String>(sp(ctx).getStringSet("burned", emptySet()) ?: emptySet())
        set.add(id)
        sp(ctx).edit().putStringSet("burned", set).apply()
    }

    fun contacts(ctx: Context): List<Pair<String, String>> {
        val set = sp(ctx).getStringSet("hanko_contacts", emptySet()) ?: emptySet()
        val out = ArrayList<Pair<String, String>>()
        for (e in set) {
            val i = e.indexOf('\u0001')
            if (i > 0) out.add(Pair(e.substring(0, i), e.substring(i + 1)))
        }
        return out.sortedBy { it.first }
    }

    fun addContact(ctx: Context, name: String, key: String) {
        val set = HashSet<String>()
        for (c in contacts(ctx)) if (c.first != name) set.add(c.first + "\u0001" + c.second)
        set.add(name + "\u0001" + key.trim())
        sp(ctx).edit().putStringSet("hanko_contacts", set).apply()
    }

    fun removeContact(ctx: Context, name: String) {
        val set = HashSet<String>()
        for (c in contacts(ctx)) if (c.first != name) set.add(c.first + "\u0001" + c.second)
        sp(ctx).edit().putStringSet("hanko_contacts", set).apply()
    }

    /** Silent wipe used by the duress PIN: removes vault images and temporary files, keeps the PIN and settings. */
    fun wipeVaultOnly(ctx: Context) {
        ImageIO.vaultDir(ctx).listFiles()?.forEach { it.delete() }
        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    /** Wipes everything the app owns: vault images, cache, PIN and settings. Gallery PNGs are not touched. */
    fun wipeAll(ctx: Context) {
        ImageIO.vaultDir(ctx).listFiles()?.forEach { it.delete() }
        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        Hanko.deleteKey()
        sp(ctx).edit().clear().commit()
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
