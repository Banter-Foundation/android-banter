package com.banter.android.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
 * Service also publishes downscaled frames for the in-call stream preview.
 */
@Singleton
class ScreenShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ScreenShareManager"

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing

    private val _previewFrame = MutableStateFlow<Bitmap?>(null)
    val previewFrame: StateFlow<Bitmap?> = _previewFrame

    init {
        // Bridge service lifecycle → DI singleton state.
        serviceSharingCallback = { active ->
            _isSharing.value = active
            if (!active) {
                _previewFrame.value?.recycle()
                _previewFrame.value = null
            }
            Log.i(TAG, if (active) "Go Live active (system share indicator on)" else "Go Live stopped")
        }
        servicePreviewCallback = { bitmap ->
            val previous = _previewFrame.value
            _previewFrame.value = bitmap
            if (previous != null && previous !== bitmap && !previous.isRecycled) {
                previous.recycle()
            }
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
        _previewFrame.value?.recycle()
        _previewFrame.value = null
    }

    companion object {
        @Volatile
        private var serviceSharingCallback: ((Boolean) -> Unit)? = null

        @Volatile
        private var servicePreviewCallback: ((Bitmap) -> Unit)? = null

        fun notifyStartedFromService() {
            serviceSharingCallback?.invoke(true)
        }

        fun notifyStoppedFromService() {
            serviceSharingCallback?.invoke(false)
        }

        fun publishPreviewFrame(bitmap: Bitmap) {
            servicePreviewCallback?.invoke(bitmap)
        }
    }
}
