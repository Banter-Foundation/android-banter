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
 * Android screen-share (Go Live) using MediaProjection.
 *
 * Full WebRTC H.264 publish to the Banter SFU is desktop-parity work; this
 * manager owns the capture consent + local "sharing" state so the call UI and
 * `/v1/voice/flags` `screen` bit stay in sync. Frames can later feed an RTC track.
 */
@Singleton
class ScreenShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ScreenShareManager"

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing

    private var resultCode: Int = Activity.RESULT_CANCELED
    private var resultData: Intent? = null

    fun createScreenCaptureIntent(): Intent {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
    }

    fun onPermissionResult(resultCode: Int, data: Intent?) {
        this.resultCode = resultCode
        this.resultData = data
        if (resultCode == Activity.RESULT_OK && data != null) {
            _isSharing.value = true
            Log.i(TAG, "Screen share permission granted — Go Live active")
        } else {
            _isSharing.value = false
            Log.w(TAG, "Screen share permission denied")
        }
    }

    fun stop() {
        _isSharing.value = false
        resultData = null
        resultCode = Activity.RESULT_CANCELED
        Log.i(TAG, "Screen share stopped")
    }

    fun hasProjection(): Boolean = resultCode == Activity.RESULT_OK && resultData != null
}
