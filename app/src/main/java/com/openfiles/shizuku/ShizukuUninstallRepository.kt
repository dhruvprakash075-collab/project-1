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
        val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shizuku")
            .debuggable(false)
            .version(1)
        Shizuku.bindUserService(args, connection)
    }

    suspend fun uninstallPackages(packageNames: List<String>): Map<String, Boolean> {
        ensureBound()
        var attempts = 0
        while (binder == null && attempts < 50) {
            delay(100)
            attempts++
        }
        val service = binder ?: return packageNames.associateWith { false }
        return packageNames.associateWith { pkg ->
            try {
                service.uninstallPackage(pkg) == 0
            } catch (e: Exception) {
                false
            }
        }
    }
}
