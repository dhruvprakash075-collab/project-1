package com.openfiles.core.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openfiles.core.data.db.FileDao
import com.openfiles.core.data.db.LockedFileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lockedFolderDataStore by preferencesDataStore(name = "locked_folder")

/**
 * AES-256-GCM file encryption using the Android Keystore directly (KeyStore + Cipher) -- NOT the
 * androidx.security:security-crypto ("JetSec") wrapper. That library was deprecated at its
 * 1.1.0-alpha07 release even though a "stable" 1.1.0 also exists, which conflicts with this
 * project's no-legacy-dependency rule (verified live 2 Jul 2026; see ADR-022). The key never
 * leaves the device's secure hardware (StrongBox/TEE when available) and needs zero new
 * Gradle dependencies. The PIN itself is never stored -- only its SHA-256 hash, in DataStore
 * (consistent with the rest of the app's settings storage).
 */
@Singleton
class SecurityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FileDao,
) {
    private val keyAlias = "openfiles_locked_folder_key"
    private val pinHashKey = stringPreferencesKey("pin_hash")

    private val lockedDir: File get() = File(context.filesDir, "locked").apply { mkdirs() }

    val lockedItems: Flow<List<LockedFileEntity>> = dao.lockedItems()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    suspend fun hasPin(): Boolean =
        context.lockedFolderDataStore.data.map { it[pinHashKey] != null }.first()

    suspend fun setPin(pin: String) {
        context.lockedFolderDataStore.edit { it[pinHashKey] = hash(pin) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.lockedFolderDataStore.data.map { it[pinHashKey] }.first()
        return stored != null && stored == hash(pin)
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun lockFile(source: File) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encryptedName = "${System.currentTimeMillis()}_${source.name}.enc"
        val destination = File(lockedDir, encryptedName)
        destination.outputStream().use { output ->
            output.write(cipher.iv.size)
            output.write(cipher.iv)
            source.inputStream().use { input ->
                CipherOutputStream(output, cipher).use { cipherOut -> input.copyTo(cipherOut) }
            }
        }
        source.delete()
        dao.addLockedItem(LockedFileEntity(originalPath = source.absolutePath, encryptedFileName = encryptedName))
    }

    suspend fun unlockFile(item: LockedFileEntity) = withContext(Dispatchers.IO) {
        val encryptedFile = File(lockedDir, item.encryptedFileName)
        val original = File(item.originalPath)
        original.parentFile?.mkdirs()
        encryptedFile.inputStream().use { input ->
            val ivSize = input.read()
            val iv = ByteArray(ivSize)
            input.read(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            }
            original.outputStream().use { output ->
                CipherInputStream(input, cipher).use { cipherIn -> cipherIn.copyTo(output) }
            }
        }
        encryptedFile.delete()
        dao.removeLockedItem(item)
    }
}
