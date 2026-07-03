package com.openfiles.shizuku

import android.os.Process

/**
 * Runs in a separate process with the privilege Shizuku/Sui granted (shell via ADB, or root) --
 * NOT inside the app's normal process. Shizuku instantiates this by ComponentName reflection via
 * bindUserService; do not declare it as a <service> in AndroidManifest.xml.
 *
 * Deliberately shells out to the documented `pm uninstall` command instead of hidden
 * IPackageManager reflection APIs: one stable, public command-line surface instead of a private
 * API that can silently break across Android versions.
 */
class UserService : IUserService.Stub() {

    override fun uninstallPackage(packageName: String): String = try {
        val process = ProcessBuilder("pm", "uninstall", packageName)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        "exitCode=$exitCode\n$output"
    } catch (e: Exception) {
        "exitCode=-1\n${e.message.orEmpty()}"
    }

    override fun destroy() {
        Process.killProcess(Process.myPid())
    }
}
