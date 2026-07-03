package com.openfiles.core.data

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.openfiles.core.data.db.FileDao
import com.openfiles.core.data.db.SmbConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

data class SmbFileItem(val name: String, val isDirectory: Boolean, val sizeBytes: Long, val lastModified: Long)

@Singleton
class SmbRepository @Inject constructor(
    private val dao: FileDao,
    private val securityRepository: SecurityRepository,
) {
    private val client by lazy { SMBClient() }

    val connections: Flow<List<SmbConnection>> = dao.smbConnections()

    suspend fun addConnection(label: String, host: String, shareName: String, username: String, password: String, domain: String? = null, port: Int = 445) =
        withContext(Dispatchers.IO) {
            dao.addSmbConnection(
                SmbConnection(
                    label = label,
                    host = host,
                    shareName = shareName,
                    username = username,
                    domain = domain,
                    port = port,
                    encryptedPassword = securityRepository.encryptString(password),
                ),
            )
        }

    suspend fun removeConnection(id: Long) = withContext(Dispatchers.IO) { dao.removeSmbConnection(id) }

    suspend fun list(connection: SmbConnection, path: String): Result<List<SmbFileItem>> = withContext(Dispatchers.IO) {
        runCatching {
            withShare(connection) { share ->
                val dir = if (path.isBlank()) "\\" else path
                share.list(dir).filterNot { it.fileName == "." || it.fileName == ".." }.map { entry ->
                    SmbFileItem(
                        name = entry.fileName,
                        isDirectory = (entry.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L,
                        sizeBytes = entry.endOfFile,
                        lastModified = entry.lastWriteTime.toEpochMillis(),
                    )
                }
            }
        }
    }

    suspend fun download(connection: SmbConnection, remotePath: String, destination: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            withShare(connection) { share ->
                share.openFile(
                    remotePath,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null,
                ).use { file ->
                    destination.outputStream().use { out -> file.inputStream.copyTo(out) }
                }
            }
            destination
        }
    }

    private fun <T> withShare(connection: SmbConnection, block: (DiskShare) -> T): T {
        client.connect(connection.host, connection.port).use { conn ->
            val password = securityRepository.decryptString(connection.encryptedPassword)
            val auth = AuthenticationContext(connection.username, password.toCharArray(), connection.domain)
            conn.authenticate(auth).connectShare(connection.shareName).use { share ->
                return block(share as DiskShare)
            }
        }
    }
}
