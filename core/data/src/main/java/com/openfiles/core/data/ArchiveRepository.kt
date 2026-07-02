package com.openfiles.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ArchiveEntryInfo(val name: String, val isDirectory: Boolean, val sizeBytes: Long)

/**
 * Zip via zip4j (handles AES-encrypted zips), everything else (tar/tar.gz/7z read path) via
 * Commons Compress. Extraction always writes into the app-chosen destination directory; archives
 * are read-only otherwise (no in-place editing in v1).
 */
@Singleton
class ArchiveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun listZipEntries(archive: File, password: CharArray? = null): List<ArchiveEntryInfo> {
        val zip = ZipFile(archive)
        if (zip.isEncrypted && password != null) zip.setPassword(password)
        return zip.fileHeaders.map { ArchiveEntryInfo(it.fileName, it.isDirectory, it.uncompressedSize) }
    }

    suspend fun extractZip(archive: File, destination: File, password: CharArray? = null) =
        withContext(Dispatchers.IO) {
            val zip = ZipFile(archive)
            if (zip.isEncrypted && password != null) zip.setPassword(password)
            zip.extractAll(destination.absolutePath)
        }

    suspend fun listTarEntries(archive: File): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArchiveEntryInfo>()
        TarArchiveInputStream(archive.inputStream()).use { tar ->
            var entry = tar.getNextEntry()
            while (entry != null) {
                entries += ArchiveEntryInfo(entry.name, entry.isDirectory, entry.size)
                entry = tar.getNextEntry()
            }
        }
        entries
    }

    suspend fun extractGeneric(archive: File, destination: File) = withContext(Dispatchers.IO) {
        destination.mkdirs()
        val input: ArchiveInputStream<out ArchiveEntry> =
            ArchiveStreamFactory().createArchiveInputStream(archive.inputStream().buffered())
        input.use {
            var entry = it.getNextEntry()
            while (entry != null) {
                val outFile = File(destination, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> it.copyTo(out) }
                }
                entry = it.getNextEntry()
            }
        }
    }
}
