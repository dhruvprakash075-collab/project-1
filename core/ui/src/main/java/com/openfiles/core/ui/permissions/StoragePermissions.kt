package com.openfiles.core.ui.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * Default to SAF; only request all-files access for true file-manager mode. Never block the app's
 * core functionality on a missing permission — always offer the SAF fallback path instead.
 */
object StoragePermissions {
    fun hasAllFiles(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    fun requestAllFiles(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
