package com.openfiles.core.data

import android.content.Context
import android.os.Environment
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

data class DuplicateGroup(val hash: String, val sizeBytes: Long, val files: List<File>)

/**
 * Finds duplicate files under external storage: groups by size first (cheap, no I/O), then hashes
 * only files that share a size with at least one other file, via [RustHashRepository] (BLAKE3,
 * Kotlin SHA-256 fallback -- see ADR-027). Reuses the same single-pass-walk approach as
 * [StorageRepository] (ADR-020) rather than a second, separate indexing pass.
 */
@Singleton
class DuplicateFinderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hashRepository: RustHashRepository,
) {
    suspend fun findDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val bySize = mutableMapOf<Long, MutableList<File>>()

        if (root.exists()) {
            Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (attrs.size() > 0) {
                        bySize.getOrPut(attrs.size()) { mutableListOf() }.add(file.toFile())
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE
            })
        }

        val byHash = mutableMapOf<String, MutableList<File>>()
        bySize.values.filter { it.size > 1 }.forEach { sameSize ->
            sameSize.forEach { file ->
                val hash = runCatching { hashRepository.hashFile(file.absolutePath) }.getOrNull()
                if (hash != null) byHash.getOrPut(hash) { mutableListOf() }.add(file)
            }
        }

        byHash.filterValues { it.size > 1 }
            .map { (hash, files) -> DuplicateGroup(hash, files.first().length(), files) }
    }
}
