package moe.lyniko.keepaliver.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    private const val PERMISSION_REQUEST_CODE = 7441

    enum class PermissionState {
        GRANTED,
        REQUESTED,
        DENIED,
        NOT_READY
    }

    fun isShizukuReady(): Boolean {
        return runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    }

    fun checkPermission(): PermissionState {
        if (!isShizukuReady()) return PermissionState.NOT_READY

        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        if (granted) return PermissionState.GRANTED

        if (runCatching { Shizuku.shouldShowRequestPermissionRationale() }.getOrDefault(false)) {
            return PermissionState.DENIED
        }

        return try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            PermissionState.REQUESTED
        } catch (e: Exception) {
            PermissionState.DENIED
        }
    }

    fun isPermissionRequestCode(requestCode: Int): Boolean {
        return requestCode == PERMISSION_REQUEST_CODE
    }
}
