package com.cartoniq.stealthtext

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.ByteArrayOutputStream

/**
 * Storm Mode: hides a SHORT encrypted text in the average brightness of 16x16 pixel blocks.
 * Block averages survive JPEG compression much better than the lowest pixel bits.
 * Each bit is repeated many times and read back by majority vote. The image is scaled to at most
 * 1280 px on its long side; resizing after hiding (for example by Instagram) still breaks it.
 */
object Storm {
    private const val BLOCK = 16
    private const val STEP = 20
    private const val HEAD_BLOCKS = 64
    private const val MIN_REPEAT = 3
    private const val MAX_REPEAT = 9
    private const val OVERHEAD = 16 + 12 + 16
    const val MAX_SIDE = 1280

    fun fittedSize(w: Int, h: Int): Pair<Int, Int> {
        val s = minOf(1f, MAX_SIDE.toFloat() / maxOf(w, h))
        return Pair(((w * s).toInt() / BLOCK) * BLOCK, ((h * s).toInt() / BLOCK) * BLOCK)
    }

    /** Rough limit for the packed message in bytes (about 10 bytes less than the plain text limit). */
    fun maxPacked(w: Int, h: Int): Int {
        val size = fittedSize(w, h)
        val blocks = (size.first / BLOCK) * (size.second / BLOCK)
        val bits = (blocks - HEAD_BLOCKS) / MIN_REPEAT
        return maxOf(0, bits / 8 - OVERHEAD)
    }

    private fun fit(src: Bitmap): Bitmap {
        val size = fittedSize(src.width, src.height)
        if (size.first < 160 || size.second < 160) throw StegoException(tr("Photo is too small."))
        val out = Bitmap.createBitmap(size.first, size.second, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        c.drawBitmap(src, Rect(0, 0, src.width, src.height), Rect(0, 0, size.first, size.second), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun luma(c: Int): Int = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000

    private fun mean(px: IntArray, w: Int, idx: Int, bw: Int): Double {
        val bx = idx % bw
        val by = idx / bw
        var s = 0L
        for (y in 0 until BLOCK) {
            val row = (by * BLOCK + y) * w + bx * BLOCK
            for (x in 0 until BLOCK) s += luma(px[row + x])
        }
        return s / (BLOCK * BLOCK).toDouble()
    }

    private fun getBit(px: IntArray, w: Int, idx: Int, bw: Int): Int =
        Math.round(mean(px, w, idx, bw) / STEP).toInt() and 1

    private fun setBit(px: IntArray, w: Int, idx: Int, bw: Int, bit: Int) {
        val m0 = mean(px, w, idx, bw)
        var q = Math.round(m0 / STEP).toInt()
        if ((q and 1) != bit) q += if (m0 / STEP >= q) 1 else -1
        while (q * STEP < 30) q += 2
        while (q * STEP > 225) q -= 2
        val target = (q * STEP).toDouble()
        val bx = idx % bw
        val by = idx / bw
        for (iter in 0 until 2) {
            val m = if (iter == 0) m0 else mean(px, w, idx, bw)
            val d = Math.round(target - m).toInt()
            if (d == 0) continue
            for (y in 0 until BLOCK) {
                val row = (by * BLOCK + y) * w + bx * BLOCK
                for (x in 0 until BLOCK) {
                    val c = px[row + x]
                    val r = (((c shr 16) and 0xFF) + d).coerceIn(0, 255)
                    val g = (((c shr 8) and 0xFF) + d).coerceIn(0, 255)
                    val b = ((c and 0xFF) + d).coerceIn(0, 255)
                    px[row + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
    }

    fun encode(src: Bitmap, text: String, password: String): Bitmap {
        if (password.length < Stego.MIN_PASSWORD) throw StegoException(tr("Use a password of at least 6 characters."))
        val plain = text.toByteArray(Charsets.UTF_8)
        if (plain.isEmpty()) throw StegoException(tr("Nothing to hide."))
        val body = Crypto.encrypt(Payload.pack(Payload.TEXT, "", plain), password)
        val img = fit(src)
        val w = img.width
        val h = img.height
        val bw = w / BLOCK
        val total = bw * (h / BLOCK)
        val bodyBits = body.size * 8
        val rep = minOf((total - HEAD_BLOCKS) / bodyBits, MAX_REPEAT)
        if (rep < MIN_REPEAT || body.size > 0xFFFF) throw StegoException(tr("Too long for Storm Mode. Keep the message short."))
        val px = IntArray(w * h)
        img.getPixels(px, 0, w, 0, 0, w, h)
        img.recycle()
        val len = body.size
        for (i in 0 until 16) {
            val bit = (len shr (15 - i)) and 1
            for (r in 0 until 4) setBit(px, w, r * 16 + i, bw, bit)
        }
        for (i in 0 until bodyBits) {
            val bit = (body[i shr 3].toInt() shr (7 - (i and 7))) and 1
            for (r in 0 until rep) setBit(px, w, HEAD_BLOCKS + r * bodyBits + i, bw, bit)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    fun decode(src: Bitmap, password: String): String {
        if (password.isEmpty()) throw StegoException(tr("Enter the password."))
        val w = src.width / BLOCK * BLOCK
        val h = src.height / BLOCK * BLOCK
        if (w < 160 || h < 160) throw StegoException(tr("No hidden message found in this image."))
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val bw = w / BLOCK
        val total = bw * (h / BLOCK)
        var len = 0
        for (i in 0 until 16) {
            var votes = 0
            for (r in 0 until 4) votes += getBit(px, w, r * 16 + i, bw)
            len = (len shl 1) or (if (votes * 2 > 4) 1 else 0)
        }
        if (len < OVERHEAD + 4 || len * 8 > (total - HEAD_BLOCKS) / MIN_REPEAT) throw StegoException(tr("No hidden message found in this image."))
        val bodyBits = len * 8
        val rep = minOf((total - HEAD_BLOCKS) / bodyBits, MAX_REPEAT)
        if (rep < MIN_REPEAT) throw StegoException(tr("No hidden message found in this image."))
        val body = ByteArray(len)
        for (i in 0 until bodyBits) {
            var votes = 0
            for (r in 0 until rep) votes += getBit(px, w, HEAD_BLOCKS + r * bodyBits + i, bw)
            if (votes * 2 > rep) body[i shr 3] = (body[i shr 3].toInt() or (1 shl (7 - (i and 7)))).toByte()
        }
        val plain = Crypto.decrypt(body, password) ?: throw StegoException(tr("Wrong password, or the image was changed too much."))
        return String(Payload.unpack(plain).data, Charsets.UTF_8)
    }

    /** Re-compresses the result as JPEG in memory and checks that the message still reads back. */
    fun jpegTest(stego: Bitmap, password: String, text: String, quality: Int): Boolean {
        val bos = ByteArrayOutputStream()
        stego.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        val bytes = bos.toByteArray()
        val back = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        return try {
            decode(back, password) == text
        } catch (e: Exception) {
            false
        } finally {
            back.recycle()
        }
    }
}
