package com.openfiles.core.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(
    val label: String,
    val packageName: String,
    val versionName: String?,
    val isSystemApp: Boolean,
    val sourceApkPath: String?,
    val sizeBytes: Long,
)

/**
 * Lists installed apps and offers uninstall (system dialog, no elevated privilege needed) plus
 * "backup APK" (copies the installed base APK to a user-visible folder). Requires
 * QUERY_ALL_PACKAGES since a file/app manager legitimately needs to see all installed apps, not
 * just ones it already interacts with via intents -- Play-reviewed, see AndroidManifest.xml.
 */
@Singleton
class AppManagerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun listInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            apps
                .filter { includeSystemApps || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .mapNotNull { info ->
                    val apkFile = info.sourceDir?.let(::File)
                    if (apkFile == null || !apkFile.exists()) return@mapNotNull null
                    val versionName = try {
                        pm.getPackageInfo(info.packageName, 0).versionName
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                    InstalledApp(
                        label = pm.getApplicationLabel(info).toString(),
                        packageName = info.packageName,
                        versionName = versionName,
                        isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        sourceApkPath = apkFile.absolutePath,
                        sizeBytes = apkFile.length(),
                    )
                }
                .sortedBy { it.label.lowercase() }
        }

    /** Copies the installed app's base APK to [destinationDir], returning the new file or null on failure. */
    suspend fun backupApk(app: InstalledApp, destinationDir: File): File? = withContext(Dispatchers.IO) {
        val source = app.sourceApkPath?.let(::File) ?: return@withContext null
        val target = File(destinationDir, "${app.packageName}_${app.versionName ?: "unknown"}.apk")
        try {
            source.copyTo(target, overwrite = true)
            target
        } catch (e: Exception) {
            target.delete()
            null
        }
    }
}
