package com.cartoniq.stealthtext

import androidx.core.content.FileProvider
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import android.provider.MediaStore

object ImageIO {
    fun imageSize(ctx: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            if (o.outWidth > 0 && o.outHeight > 0) Pair(o.outWidth, o.outHeight) else null
        } catch (e: Exception) {
            null
        }
    }

    fun loadPreview(ctx: Context, uri: Uri, maxSide: Int = 900): Bitmap? {
        val size = imageSize(ctx, uri) ?: return null
        var sample = 1
        while (maxOf(size.first, size.second) / sample > maxSide) sample *= 2
        return try {
            val o = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun loadBitmap(ctx: Context, uri: Uri, applyExif: Boolean): Bitmap? {
        return try {
            val o = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
                ?: return null
            val deg = if (applyExif) exifRotation(ctx, uri) else 0
            if (deg == 0) {
                bmp
            } else {
                val m = Matrix().apply { postRotate(deg.toFloat()) }
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (rotated !== bmp) bmp.recycle()
                rotated
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun exifRotation(ctx: Context, uri: Uri): Int {
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun savePng(ctx: Context, bmp: Bitmap, tag: String = ""): Uri? {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "stealth_${System.currentTimeMillis()}$tag.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StealthText")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val ok = try {
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            resolver.delete(uri, null, null)
            return null
        }
        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        resolver.update(uri, done, null, null)
        return uri
    }

    fun vaultDir(ctx: Context): File {
        val d = File(ctx.filesDir, "vault")
        d.mkdirs()
        return d
    }

    fun vaultFiles(ctx: Context): List<File> =
        vaultDir(ctx).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun saveToVault(ctx: Context, bmp: Bitmap, tag: String = ""): Uri? {
        val f = File(vaultDir(ctx), "v_${System.currentTimeMillis()}$tag.png")
        val ok = try {
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            f.delete()
            return null
        }
        return Uri.fromFile(f)
    }

    fun readFile(ctx: Context, uri: Uri): Pair<String, ByteArray>? {
        return try {
            var name = "file"
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) name = c.getString(i) ?: name
                }
            }
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.size > 20_000_000) null else Pair(name, bytes)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun saveToDownloads(ctx: Context, name: String, mime: String, data: ByteArray): Boolean {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StealthText")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        val ok = try {
            resolver.openOutputStream(uri)?.use { it.write(data) } != null
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            resolver.delete(uri, null, null)
            return false
        }
        val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
        resolver.update(uri, done, null, null)
        return true
    }

    fun saveToCache(ctx: Context, bmp: Bitmap): Uri? {
        ctx.cacheDir.listFiles()?.forEach { if (it.name.startsWith("cover_")) it.delete() }
        val f = File(ctx.cacheDir, "cover_${System.currentTimeMillis()}.png")
        val ok = try {
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            f.delete()
            return null
        }
        return Uri.fromFile(f)
    }

    const val AUTH = "com.cartoniq.stealthtext.fileprovider"

    /** Content URIs can be shared as they are; private files (vault, cache) go through the FileProvider. */
    fun shareUri(ctx: Context, uri: Uri): Uri {
        val p = uri.path
        return if (uri.scheme == "file" && p != null) FileProvider.getUriForFile(ctx, AUTH, File(p)) else uri
    }

    fun copyToDownloads(ctx: Context, uris: List<Uri>, mime: String): Boolean {
        val ext = if (mime.contains("jpeg")) "jpg" else "png"
        var all = true
        for ((i, u) in uris.withIndex()) {
            val bytes: ByteArray? = try {
                ctx.contentResolver.openInputStream(u)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
            if (bytes == null) {
                all = false
                continue
            }
            if (!saveToDownloads(ctx, "stealth_${System.currentTimeMillis()}_$i.$ext", mime, bytes)) all = false
        }
        return all
    }

    fun saveJpeg(ctx: Context, bmp: Bitmap): Uri? {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "clean_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StealthText")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val ok = try {
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) } ?: false
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            resolver.delete(uri, null, null)
            return null
        }
        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        resolver.update(uri, done, null, null)
        return uri
    }

    /** Short list of what hidden metadata a photo carries. */
    fun exifReport(ctx: Context, uri: Uri): List<String> {
        val out = ArrayList<String>()
        try {
            ctx.contentResolver.openInputStream(uri)?.use { s ->
                val e = ExifInterface(s)
                val ll = FloatArray(2)
                out.add(if (e.getLatLong(ll)) "Location (GPS): FOUND" else "Location (GPS): none")
                val make = e.getAttribute(ExifInterface.TAG_MAKE)
                val model = e.getAttribute(ExifInterface.TAG_MODEL)
                if (make != null || model != null) out.add("Camera: " + (make ?: "") + " " + (model ?: ""))
                val date = e.getAttribute(ExifInterface.TAG_DATETIME)
                if (date != null) out.add("Date taken: $date")
                val soft = e.getAttribute(ExifInterface.TAG_SOFTWARE)
                if (soft != null) out.add("Software: $soft")
            }
        } catch (e: Exception) {
            out.add("Could not read metadata.")
        }
        return out
    }
}
