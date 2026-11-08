package com.banter.android.media

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.banter.android.MainActivity

/**
 * Holds an active [MediaProjection] so Android shows the system red
 * status-bar "screen sharing" pill (same cue Discord uses).
 *
 * Must run as a `mediaProjection` foreground service; creating the
 * VirtualDisplay is what makes the indicator appear.
 */
class ScreenShareService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by system")
            tearDownCapture()
            stopSelf()
            ScreenShareManager.notifyStoppedFromService()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSharing()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode != Activity.RESULT_OK || data == null) {
                    Log.w(TAG, "Missing MediaProjection consent — aborting")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startAsForeground()
                if (!startProjection(resultCode, data)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopShare = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Banter is sharing your screen")
            .setContentText("Tap to return to the call · Stop to end Go Live")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openApp)
            .addAction(0, "Stop sharing", stopShare)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(resultCode: Int, data: Intent): Boolean {
        return try {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpm.getMediaProjection(resultCode, data)
                ?: run {
                    Log.e(TAG, "getMediaProjection returned null")
                    return false
                }
            mediaProjection = projection
            projection.registerCallback(projectionCallback, null)

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(720)
            val height = metrics.heightPixels.coerceAtLeast(1280)
            val density = metrics.densityDpi

            // ImageReader keeps the VirtualDisplay alive so the system
            // status-bar share indicator stays visible. Frames are drained
            // (WebRTC encode can replace this later).
            val reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener({ r ->
                r.acquireLatestImage()?.close()
            }, null)

            virtualDisplay = projection.createVirtualDisplay(
                "BanterGoLive",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )
            Log.i(TAG, "VirtualDisplay started ${width}x$height — system share pill should show")
            ScreenShareManager.notifyStartedFromService()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start MediaProjection", t)
            tearDownCapture()
            false
        }
    }

    private fun stopSharing() {
        tearDownCapture()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        ScreenShareManager.notifyStoppedFromService()
    }

    private fun tearDownCapture() {
        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Throwable) {
        }
        imageReader = null
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Throwable) {
        }
        mediaProjection = null
    }

    override fun onDestroy() {
        tearDownCapture()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Screen share",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while Banter is sharing your screen"
                setShowBadge(false)
            }
        )
    }

    companion object {
        private const val TAG = "ScreenShareService"
        private const val CHANNEL_ID = "banter_screen_share"
        private const val NOTIFICATION_ID = 39701

        const val ACTION_START = "com.banter.android.action.START_SCREEN_SHARE"
        const val ACTION_STOP = "com.banter.android.action.STOP_SCREEN_SHARE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenShareService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ScreenShareService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
