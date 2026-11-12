package com.cartoniq.stealthtext

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

object Payload {
    const val TEXT = 0
    const val FILE = 1
    const val AUDIO = 2
    const val MARK = 3

    class Item(
        val type: Int,
        val name: String,
        val data: ByteArray,
        val burn: Boolean = false,
        val id: String = "",
        val sig: ByteArray? = null,
        val signedBytes: ByteArray? = null
    ) {
        var sigStatus: String = ""
    }

    /** Layout before compression: head(type + burn flag), sigLen, sig, type, nameLen, name, data. */
    fun pack(type: Int, name: String, data: ByteArray, burn: Boolean = false, sign: Boolean = false): ByteArray {
        var nb = name.toByteArray(Charsets.UTF_8)
        if (nb.size > 200) nb = nb.copyOf(200)
        val core = ByteArray(2 + nb.size + data.size)
        core[0] = type.toByte()
        core[1] = nb.size.toByte()
        System.arraycopy(nb, 0, core, 2, nb.size)
        System.arraycopy(data, 0, core, 2 + nb.size, data.size)
        val sig = if (sign) (Hanko.sign(core) ?: throw StegoException(tr("Could not sign. Open Tools, Hanko first."))) else ByteArray(0)
        val raw = ByteArray(2 + sig.size + core.size)
        raw[0] = (type or (if (burn) 0x80 else 0)).toByte()
        raw[1] = sig.size.toByte()
        System.arraycopy(sig, 0, raw, 2, sig.size)
        System.arraycopy(core, 0, raw, 2 + sig.size, core.size)
        val d = Deflater(9)
        d.setInput(raw)
        d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (!d.finished()) {
            val n = d.deflate(buf)
            out.write(buf, 0, n)
        }
        d.end()
        return out.toByteArray()
    }

    fun unpack(packed: ByteArray): Item {
        val inf = Inflater()
        val out = ByteArrayOutputStream()
        try {
            inf.setInput(packed)
            val buf = ByteArray(8192)
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
                out.write(buf, 0, n)
                if (out.size() > 100_000_000) throw StegoException("Hidden data is corrupt.")
            }
        } catch (e: DataFormatException) {
            throw StegoException("Hidden data is corrupt.")
        } finally {
            inf.end()
        }
        val raw = out.toByteArray()
        if (raw.size < 4) throw StegoException("Hidden data is corrupt.")
        val head = raw[0].toInt() and 0xFF
        val sigLen = raw[1].toInt() and 0xFF
        if (2 + sigLen + 2 > raw.size) throw StegoException("Hidden data is corrupt.")
        val sig = raw.copyOfRange(2, 2 + sigLen)
        val core = raw.copyOfRange(2 + sigLen, raw.size)
        val nameLen = core[1].toInt() and 0xFF
        if (2 + nameLen > core.size) throw StegoException("Hidden data is corrupt.")
        val name = String(core, 2, nameLen, Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(raw)
        val id = digest.take(8).joinToString("") { "%02x".format(it) }
        return Item(
            head and 0x3F, name, core.copyOfRange(2 + nameLen, core.size),
            (head and 0x80) != 0, id,
            if (sigLen > 0) sig else null,
            if (sigLen > 0) core else null
        )
    }

    fun xor(a: ByteArray, b: ByteArray): ByteArray {
        if (a.size != b.size) throw StegoException(tr("The two images do not belong together."))
        val r = ByteArray(a.size)
        for (i in a.indices) r[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        return r
    }
}
