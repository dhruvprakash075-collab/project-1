package com.openfiles.core.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.openfiles.core.common.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads directories from two sources:
 *  - [listPath] real filesystem via Java NIO2 (requires core library desugaring on minSdk 24).
 *  - [listTree] a SAF document tree (scoped storage, no special permission needed).
 * File ops (copy/move/delete/rename/create) operate on NIO2 [Path]s, which is what "All files
 * access" mode gives us; SAF-tree mutation is intentionally out of scope for v1 (browse + open only).
 */
@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dirsFirst =
        compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }

    suspend fun listPath(dir: Path): List<FileItem> = withContext(Dispatchers.IO) {
        Files.newDirectoryStream(dir).use { stream ->
            stream.mapNotNull { p ->
                runCatching {
                    val a = Files.readAttributes(p, BasicFileAttributes::class.java)
                    FileItem(
                        name = p.fileName.toString(),
                        uri = Uri.fromFile(p.toFile()),
                        isDirectory = a.isDirectory,
                        sizeBytes = a.size(),
                        lastModified = a.lastModifiedTime().toMillis(),
                        mimeType = runCatching { Files.probeContentType(p) }.getOrNull(),
                        path = p.toString(),
                    )
                }.getOrNull()
            }.sortedWith(dirsFirst)
        }
    }

    suspend fun listTree(treeUri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        root.listFiles().map { doc ->
            FileItem(
                name = doc.name.orEmpty(),
                uri = doc.uri,
                isDirectory = doc.isDirectory,
                sizeBytes = doc.length(),
                lastModified = doc.lastModified(),
                mimeType = doc.type,
            )
        }.sortedWith(dirsFirst)
    }

    suspend fun createFolder(parent: Path, name: String): Path = withContext(Dispatchers.IO) {
        Files.createDirectory(parent.resolve(name))
    }

    suspend fun createFile(parent: Path, name: String): Path = withContext(Dispatchers.IO) {
        Files.createFile(parent.resolve(name))
    }

    suspend fun rename(item: Path, newName: String): Path = withContext(Dispatchers.IO) {
        Files.move(item, item.resolveSibling(newName))
    }

    suspend fun delete(items: List<Path>) = withContext(Dispatchers.IO) {
        items.forEach { path -> path.toFile().deleteRecursively() }
    }

    suspend fun copy(items: List<Path>, destination: Path) = withContext(Dispatchers.IO) {
        items.forEach { src -> src.toFile().copyRecursively(File(destination.toFile(), src.fileName.toString()), overwrite = true) }
    }

    suspend fun move(items: List<Path>, destination: Path) = withContext(Dispatchers.IO) {
        items.forEach { src -> Files.move(src, destination.resolve(src.fileName)) }
    }
}
