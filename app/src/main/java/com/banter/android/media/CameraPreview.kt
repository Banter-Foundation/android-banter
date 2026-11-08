package com.banter.android.media

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.banter.android.ui.theme.BanterColors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local CameraX preview for the call stage (Discord-style self video).
 */
@Composable
fun CameraPreview(
    frontFacing: Boolean,
    mirrored: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var error by remember { mutableStateOf<String?>(null) }
    val bound = remember { AtomicBoolean(false) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Mirror front camera like Discord / FaceTime self-view.
                    scaleX = if (mirrored && frontFacing) -1f else 1f
                },
            update = { previewView ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val selector = if (frontFacing) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        bound.set(true)
                        error = null
                    } catch (t: Throwable) {
                        error = t.message ?: "Camera unavailable"
                        bound.set(false)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        error?.let { msg ->
            Text(
                msg,
                color = BanterColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        DisposableEffect(frontFacing) {
            onDispose {
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (_: Throwable) {
                }
                bound.set(false)
            }
        }
    }
}
