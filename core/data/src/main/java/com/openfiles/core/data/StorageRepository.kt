package com.openfiles.core.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import javax.inject.Singleton

enum class StorageCategory { IMAGES, VIDEO, AUDIO, DOCUMENTS, APPS, DOWNLOADS, OTHER }

data class CategoryUsage(val category: StorageCategory, val sizeBytes: Long, val fileCount: Int)

data class StorageSummary(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categories: List<CategoryUsage>,
)

/**
 * Single-pass filesystem walk that buckets every file under external storage into one of the
 * plan's storage-dashboard categories by extension (or by living inside Download/ for Downloads).
 * v1 does not drill down into a category's individual files/folders -- deferred (see ADR-020).
 */
@Singleton
class StorageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp")
    private val videoExt = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi")
    private val audioExt = setOf("mp3", "m4a", "wav", "ogg", "flac", "aac")
    private val docExt = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")

    suspend fun summarize(): StorageSummary = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val totals = mutableMapOf<StorageCategory, Long>().withDefault { 0L }
        val counts = mutableMapOf<StorageCategory, Int>().withDefault { 0 }

        if (root.exists()) {
            Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val category = categorize(file.toFile(), downloadsDir)
                    totals[category] = totals.getValue(category) + attrs.size()
                    counts[category] = counts.getValue(category) + 1
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    Log.w(TAG, "Skipping unreadable path while summarizing storage: $file", exc)
                    return FileVisitResult.CONTINUE
                }
            })
        }

        val statFs = StatFs(root.path)
        val total = statFs.totalBytes
        val free = statFs.availableBytes

        StorageSummary(
            totalBytes = total,
            usedBytes = total - free,
            freeBytes = free,
            categories = StorageCategory.entries.map { cat ->
                CategoryUsage(cat, totals.getValue(cat), counts.getValue(cat))
            },
        )
    }

    private fun categorize(file: File, downloadsDir: File): StorageCategory {
        if (file.parentFile?.absolutePath?.startsWith(downloadsDir.absolutePath) == true) return StorageCategory.DOWNLOADS
        return when (file.extension.lowercase()) {
            in imageExt -> StorageCategory.IMAGES
            in videoExt -> StorageCategory.VIDEO
            in audioExt -> StorageCategory.AUDIO
            in docExt -> StorageCategory.DOCUMENTS
            "apk" -> StorageCategory.APPS
            else -> StorageCategory.OTHER
        }
    }

    private companion object {
        const val TAG = "StorageRepository"
    }
}
