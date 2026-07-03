package com.openfiles.shizuku

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import rikka.shizuku.Shizuku

enum class ShizukuState { UNAVAILABLE, NEEDS_PERMISSION, READY }

private const val REQUEST_CODE = 9100

/**
 * Runtime Shizuku availability + permission state. Every Shizuku-gated feature must render its
 * normal, non-elevated behavior in the UNAVAILABLE state -- Shizuku is never a hard requirement
 * to use the app (Ring 3 F4 rule).
 */
object ShizukuChecker {
    fun currentState(): ShizukuState {
        if (!Shizuku.pingBinder()) return ShizukuState.UNAVAILABLE
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuState.READY
        } else {
            ShizukuState.NEEDS_PERMISSION
        }
    }

    fun requestPermission() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }
}

@Composable
fun rememberShizukuState(): ShizukuState {
    var state by remember { mutableStateOf(ShizukuChecker.currentState()) }

    DisposableEffect(Unit) {
        val binderListener = Shizuku.OnBinderReceivedListener { state = ShizukuChecker.currentState() }
        val binderDeadListener = Shizuku.OnBinderDeadListener { state = ShizukuState.UNAVAILABLE }
        val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                state = if (grantResult == PackageManager.PERMISSION_GRANTED) ShizukuState.READY else ShizukuState.UNAVAILABLE
            }
        }
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        onDispose {
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }
    return state
}
