package com.openfiles.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ImageEditOp { ROTATE_LEFT, ROTATE_RIGHT, GRAYSCALE }

/**
 * v1 image editing: 90-degree rotation and a grayscale filter, applied in-memory via
 * android.graphics (zero new dependencies) and written back to the original file. Interactive
 * freeform cropping needs real drag-handle UI and is deliberately out of scope here.
 */
@Singleton
class ImageEditRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun apply(uri: Uri, op: ImageEditOp): Boolean = withContext(Dispatchers.IO) {
        val original = loadBitmap(uri) ?: return@withContext false
        val edited = when (op) {
            ImageEditOp.ROTATE_LEFT -> rotate(original, -90f)
            ImageEditOp.ROTATE_RIGHT -> rotate(original, 90f)
            ImageEditOp.GRAYSCALE -> grayscale(original)
        }
        saveBitmap(uri, edited)
    }

    private fun loadBitmap(uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun grayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun saveBitmap(uri: Uri, bitmap: Bitmap): Boolean {
        val path = uri.path
        val format = if (path?.endsWith(".png", ignoreCase = true) == true) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        return if (uri.scheme == "file" && path != null) {
            try {
                FileOutputStream(File(path)).use { out -> bitmap.compress(format, 95, out) }
                true
            } catch (e: Exception) {
                false
            }
        } else {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out -> bitmap.compress(format, 95, out) } != null
            } catch (e: Exception) {
                false
            }
        }
    }
}
