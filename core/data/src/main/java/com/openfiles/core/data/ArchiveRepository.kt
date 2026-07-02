package com.openfiles.core.data

import android.content.Context
import com.github.junrar.Archive
import com.github.junrar.Junrar
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
 * Zip via zip4j (handles AES-encrypted zips, and archive creation), RAR extraction via junrar
 * (RAR 4 and lower only -- junrar does not support RAR5, see ADR-018), everything else (tar/tar.gz
 * read path) via Commons Compress. Extraction always writes into the app-chosen destination
 * directory; only zip supports in-app creation in v1 (RAR/tar creation is out of scope).
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

    /** Creates a zip at [destinationZip] containing every file/folder in [sources]. */
    suspend fun createZip(sources: List<File>, destinationZip: File) = withContext(Dispatchers.IO) {
        val zip = ZipFile(destinationZip)
        sources.forEach { source ->
            if (source.isDirectory) zip.addFolder(source) else zip.addFile(source)
        }
    }

    suspend fun listRarEntries(archive: File): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        Archive(archive).use { rar ->
            rar.fileHeaders.map { ArchiveEntryInfo(it.fileName, it.isDirectory, it.fullUnpackSize) }
        }
    }

    suspend fun extractRar(archive: File, destination: File) = withContext(Dispatchers.IO) {
        destination.mkdirs()
        Junrar.extract(archive, destination)
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
