package com.openfiles.core.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the Rust BLAKE3 hasher (native/rust-core, built via cargo-ndk + uniffi -- see ADR-027).
 * Falls back to a pure-Kotlin SHA-256 hash (java.security, zero new deps) if the native library
 * is missing or fails to load for this ABI. This fallback is a deliberate, logged exception to the
 * no-fallback-code clause because it reflects a genuine hardware/ABI capability gap, not a design
 * shortcut -- every other hasher call site in the app only ever talks to this repository, never to
 * the native binding directly.
 */
@Singleton
class RustHashRepository @Inject constructor() {

    private val nativeAvailable: Boolean by lazy {
        try {
            System.loadLibrary("rustcore")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "rustcore native library unavailable, falling back to Kotlin SHA-256", e)
            false
        }
    }

    suspend fun hashFile(path: String): String = withContext(Dispatchers.IO) {
        if (nativeAvailable) {
            try {
                return@withContext uniffi.rustcore.hashFile(path)
            } catch (e: Exception) {
                Log.w(TAG, "Native hash_file failed for $path, falling back to Kotlin SHA-256", e)
            }
        }
        sha256Fallback(path)
    }

    private fun sha256Fallback(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "RustHashRepository"
        const val DEFAULT_BUFFER_SIZE = 8192
    }
}
