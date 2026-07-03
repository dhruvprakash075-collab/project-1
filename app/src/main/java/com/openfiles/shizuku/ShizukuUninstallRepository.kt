package com.openfiles.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Binds the Shizuku user service (UserService.kt) to run privileged package uninstalls without
 * the per-app system confirmation dialog. Lives in the app module, not :core:data, because it
 * references UserService, an app-module class Shizuku instantiates by ComponentName.
 */
@Singleton
class ShizukuUninstallRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private var binder: IUserService? = null
    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shizuku")
            .debuggable(false)
            .version(1)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = IUserService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binder = null
        }
    }

    private fun ensureBound() {
        if (binder != null) return
        Shizuku.bindUserService(args, connection)
    }

    private fun unbind() {
        runCatching { binder?.destroy() }
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        binder = null
    }

    suspend fun uninstallPackages(packageNames: List<String>): Map<String, Boolean> {
        try {
            ensureBound()
            var attempts = 0
            while (binder == null && attempts < 50) {
                delay(100)
                attempts++
            }
            val service = binder ?: return packageNames.associateWith { false }
            return packageNames.associateWith { pkg ->
                try {
                    val result = service.uninstallPackage(pkg)
                    result.lineSequence().firstOrNull()?.trim() == "exitCode=0" &&
                        result.lineSequence().drop(1).joinToString("\n").contains("Success", ignoreCase = true)
                } catch (e: Exception) {
                    false
                }
            }
        } finally {
            unbind()
        }
    }
}
