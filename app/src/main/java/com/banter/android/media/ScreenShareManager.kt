package com.banter.android.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android screen-share (Go Live).
 *
 * Consent → [ScreenShareService] (mediaProjection FGS + VirtualDisplay).
 * That is what makes Android show the red status-bar share pill, same as Discord.
 */
@Singleton
class ScreenShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ScreenShareManager"

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing

    init {
        // Bridge service lifecycle → DI singleton state.
        serviceSharingCallback = { active ->
            _isSharing.value = active
            Log.i(TAG, if (active) "Go Live active (system share indicator on)" else "Go Live stopped")
        }
    }

    fun createScreenCaptureIntent(): Intent {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
    }

    fun onPermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            ScreenShareService.start(context, resultCode, data)
        } else {
            _isSharing.value = false
            Log.w(TAG, "Screen share permission denied")
        }
    }

    fun stop() {
        ScreenShareService.stop(context)
        _isSharing.value = false
    }

    companion object {
        @Volatile
        private var serviceSharingCallback: ((Boolean) -> Unit)? = null

        fun notifyStartedFromService() {
            serviceSharingCallback?.invoke(true)
        }

        fun notifyStoppedFromService() {
            serviceSharingCallback?.invoke(false)
        }
    }
}
