package app.deltacord.manager.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import java.io.File

/**
 * Turns a picked image into an adaptive icon foreground layer.
 *
 * Adaptive icon foregrounds are 108dp, but launchers only show roughly the middle 72dp
 * through their mask. The image is center-cropped to a square and drawn into that
 * middle area, so the whole picture stays visible. The rest is transparent and shows
 * the icon background color.
 */
object CustomIcon {
    private const val SIZE = 432 // 108dp at xxxhdpi
    private const val VISIBLE = 288 // 72dp at xxxhdpi

    fun save(context: Context, uri: Uri, target: File) {
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Could not decode image")

        val side = minOf(source.width, source.height)
        val crop = Rect(
            (source.width - side) / 2,
            (source.height - side) / 2,
            (source.width - side) / 2 + side,
            (source.height - side) / 2 + side,
        )
        val offset = (SIZE - VISIBLE) / 2
        val dest = Rect(offset, offset, offset + VISIBLE, offset + VISIBLE)

        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(source, crop, dest, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        source.recycle()

        target.parentFile?.mkdirs()
        target.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        out.recycle()
    }

    /** Parses a #RRGGBB / #AARRGGBB string, or returns null. */
    fun parseColor(hex: String): Int? = try {
        android.graphics.Color.parseColor(hex.trim().let { if (it.startsWith("#")) it else "#$it" })
    } catch (_: IllegalArgumentException) {
        null
    }
}
