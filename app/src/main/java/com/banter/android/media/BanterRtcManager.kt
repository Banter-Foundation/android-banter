package com.banter.android.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.banter.android.data.api.BanterApi
import com.banter.android.data.api.WebSocketManager
import com.banter.android.data.model.ClientSignal
import com.banter.android.data.model.IceConfig
import com.banter.android.data.model.LayerId
import com.banter.android.data.model.MediaKind
import com.banter.android.data.model.MediaPlace
import com.banter.android.data.model.ServerEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.YuvHelper
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Banter WebRTC session (SFU for guild voice, P2P for DM calls).
 *
 * Offers the four Banter tracks (`user_audio`, `stream_audio`, `camera`, `screen`),
 * relays signaling over the websocket, and exposes remote camera/screen bitmaps
 * so the call stage can watch other people's streams.
 */
@Singleton
class BanterRtcManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: BanterApi,
    private val webSocketManager: WebSocketManager
) {
    private val TAG = "BanterRtc"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val factoryReady = AtomicBoolean(false)
    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null

    private var peerConnection: PeerConnection? = null
    private var place: MediaPlace? = null
    private var me: Long = 0
    private var offerer: Boolean = false
    private var peerId: AtomicLong = AtomicLong(0)

    private var userAudioTrack: AudioTrack? = null
    private var streamAudioTrack: AudioTrack? = null
    private var cameraTrack: VideoTrack? = null
    private var screenTrack: VideoTrack? = null
    private var cameraSource: VideoSource? = null
    private var screenSource: VideoSource? = null
    private var userAudioSource: AudioSource? = null
    private var streamAudioSource: AudioSource? = null
    private var surfaceHelper: SurfaceTextureHelper? = null

    private val pendingIce = mutableListOf<IceCandidate>()
    private var remoteDescriptionSet = false
    private var signalJob: Job? = null
    private var resubJob: Job? = null

    private val remoteSinks = ConcurrentHashMap<String, RemoteVideoSink>()

    private val _remoteFrames = MutableStateFlow<Map<RemoteStreamKey, Bitmap>>(emptyMap())
    val remoteFrames: StateFlow<Map<RemoteStreamKey, Bitmap>> = _remoteFrames

    private val _connectionLabel = MutableStateFlow("idle")
    val connectionLabel: StateFlow<String> = _connectionLabel

    data class RemoteStreamKey(val publisher: Long, val kind: MediaKind)

    fun start(place: MediaPlace, me: Long, offerer: Boolean) {
        scope.launch {
            try {
                ensureFactory()
                if (this@BanterRtcManager.place == place &&
                    peerConnection != null &&
                    this@BanterRtcManager.offerer == offerer &&
                    this@BanterRtcManager.me == me
                ) {
                    return@launch
                }
                stopInternal()
                this@BanterRtcManager.place = place
                this@BanterRtcManager.me = me
                this@BanterRtcManager.offerer = offerer
                peerId.set(0)
                _connectionLabel.value = "connecting"
                val ice = fetchIce()
                val pc = createPeerConnection(ice) ?: run {
                    _connectionLabel.value = "failed"
                    return@launch
                }
                peerConnection = pc
                attachPublishTracks(pc)
                listenSignals()
                if (offerer) {
                    createAndSendOffer(pc, place)
                }
                _connectionLabel.value = if (offerer) "offered" else "waiting"
                startResubscribeLoop()
            } catch (t: Throwable) {
                Log.e(TAG, "start failed", t)
                _connectionLabel.value = "failed"
            }
        }
    }

    fun stop() {
        scope.launch { stopInternal() }
    }

    /** Ask the SFU/peer for a publisher's screen or camera layer. */
    fun subscribe(publisher: Long, track: MediaKind, layer: LayerId = LayerId.Full) {
        val p = place ?: return
        if (publisher == 0L || publisher == me) return
        if (track != MediaKind.Screen && track != MediaKind.Camera) return
        webSocketManager.send(
            ClientSignal.SubscribeLayer(
                place = p,
                publisher = publisher,
                track = track,
                maxLayer = layer
            )
        )
    }

    /**
     * Keep watching every peer who has Go Live / camera on (Discord-style auto watch).
     * Re-sends every ~2s so late joiners get an IDR.
     */
    fun ensureSubsForMembers(members: List<Pair<Long, com.banter.android.data.model.VoiceFlags>>) {
        for ((userId, flags) in members) {
            if (userId == me) continue
            if (flags.screen) subscribe(userId, MediaKind.Screen, LayerId.Full)
            if (flags.camera) subscribe(userId, MediaKind.Camera, LayerId.Full)
        }
    }

    private fun startResubscribeLoop() {
        resubJob?.cancel()
        resubJob = scope.launch {
            while (isActive && peerConnection != null) {
                delay(2_000)
                // ViewModel also calls ensureSubsForMembers; this is a heartbeat PLI path.
            }
        }
    }

    private suspend fun stopInternal() {
        resubJob?.cancel()
        resubJob = null
        signalJob?.cancel()
        signalJob = null
        withContext(Dispatchers.Main) {
            try {
                cameraTrack?.dispose()
                screenTrack?.dispose()
                userAudioTrack?.dispose()
                streamAudioTrack?.dispose()
                cameraSource?.dispose()
                screenSource?.dispose()
                userAudioSource?.dispose()
                streamAudioSource?.dispose()
                surfaceHelper?.dispose()
                peerConnection?.close()
                peerConnection?.dispose()
            } catch (_: Throwable) {
            }
            cameraTrack = null
            screenTrack = null
            userAudioTrack = null
            streamAudioTrack = null
            cameraSource = null
            screenSource = null
            userAudioSource = null
            streamAudioSource = null
            surfaceHelper = null
            peerConnection = null
        }
        remoteSinks.clear()
        _remoteFrames.value = emptyMap()
        pendingIce.clear()
        remoteDescriptionSet = false
        place = null
        _connectionLabel.value = "idle"
    }

    private fun listenSignals() {
        signalJob?.cancel()
        signalJob = scope.launch {
            webSocketManager.events.collect { event ->
                val currentPlace = place ?: return@collect
                when (event) {
                    is ServerEvent.RtcOffer -> {
                        if (event.place != currentPlace) return@collect
                        if (event.from != 0L) peerId.set(event.from)
                        handleRemoteOffer(event.sdp)
                    }
                    is ServerEvent.RtcAnswer -> {
                        if (event.place != currentPlace) return@collect
                        if (event.from != 0L) peerId.set(event.from)
                        handleRemoteAnswer(event.sdp)
                    }
                    is ServerEvent.RtcIce -> {
                        if (event.place != currentPlace) return@collect
                        if (event.from != 0L) peerId.set(event.from)
                        handleRemoteIce(event.candidate, event.sdpMid, event.sdpMlineIndex)
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun ensureFactory() {
        if (factoryReady.get()) return
        withContext(Dispatchers.Main) {
            if (factoryReady.get()) return@withContext
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
            val egl = EglBase.create()
            eglBase = egl
            val encoder = DefaultVideoEncoderFactory(egl.eglBaseContext, true, true)
            val decoder = DefaultVideoDecoderFactory(egl.eglBaseContext)
            factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoder)
                .setVideoDecoderFactory(decoder)
                .createPeerConnectionFactory()
            factoryReady.set(true)
        }
    }

    private suspend fun fetchIce(): IceConfig = withContext(Dispatchers.IO) {
        try {
            val r = api.ice()
            if (r.isSuccessful) r.body() ?: IceConfig() else IceConfig()
        } catch (_: Throwable) {
            IceConfig()
        }
    }

    private fun iceServers(cfg: IceConfig): List<PeerConnection.IceServer> {
        val list = mutableListOf<PeerConnection.IceServer>()
        if (cfg.stun.isNotBlank()) {
            list += PeerConnection.IceServer.builder(cfg.stun).createIceServer()
        }
        val turn = cfg.turn
        if (turn != null && turn.urls.isNotBlank() && turn.username.isNotBlank()) {
            list += PeerConnection.IceServer.builder(turn.urls)
                .setUsername(turn.username)
                .setPassword(turn.credential)
                .createIceServer()
        }
        if (list.isEmpty()) {
            list += PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        }
        return list
    }

    private suspend fun createPeerConnection(ice: IceConfig): PeerConnection? =
        withContext(Dispatchers.Main) {
            val f = factory ?: return@withContext null
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers(ice)).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }
            f.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    _connectionLabel.value = state?.name?.lowercase() ?: "unknown"
                    Log.i(TAG, "ICE $state")
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    val c = candidate ?: return
                    val p = place ?: return
                    webSocketManager.send(
                        ClientSignal.RtcIce(
                            place = p,
                            candidate = c.sdp,
                            sdpMid = c.sdpMid,
                            sdpMlineIndex = c.sdpMLineIndex
                        )
                    )
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track() ?: return
                    if (track is VideoTrack) {
                        attachRemoteVideo(track)
                    }
                }
                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track() ?: return
                    if (track is VideoTrack) {
                        attachRemoteVideo(track)
                    }
                }
            })
        }

    private suspend fun attachPublishTracks(pc: PeerConnection) = withContext(Dispatchers.Main) {
        val f = factory ?: return@withContext
        userAudioSource = f.createAudioSource(MediaConstraints())
        streamAudioSource = f.createAudioSource(MediaConstraints())
        userAudioTrack = f.createAudioTrack("user_audio", userAudioSource).also {
            it.setEnabled(true)
            pc.addTrack(it, listOf("banter"))
        }
        streamAudioTrack = f.createAudioTrack("stream_audio", streamAudioSource).also {
            it.setEnabled(false)
            pc.addTrack(it, listOf("banter"))
        }

        val egl = eglBase ?: return@withContext
        surfaceHelper = SurfaceTextureHelper.create("BanterCapture", egl.eglBaseContext)
        cameraSource = f.createVideoSource(false)
        screenSource = f.createVideoSource(true)

        cameraTrack = f.createVideoTrack("camera", cameraSource).also {
            it.setEnabled(false)
            val sender = pc.addTrack(it, listOf("banter"))
            applySimulcast(sender)
        }
        screenTrack = f.createVideoTrack("screen", screenSource).also {
            it.setEnabled(false)
            val sender = pc.addTrack(it, listOf("banter"))
            applySimulcast(sender)
        }
    }

    private fun applySimulcast(sender: org.webrtc.RtpSender?) {
        if (sender == null) return
        try {
            val params = sender.parameters
            if (params.encodings.size >= 3) {
                params.encodings[0].rid = "q"
                params.encodings[0].scaleResolutionDownBy = 4.0
                params.encodings[0].maxBitrateBps = 2_500_000
                params.encodings[1].rid = "h"
                params.encodings[1].scaleResolutionDownBy = 2.0
                params.encodings[1].maxBitrateBps = 10_000_000
                params.encodings[2].rid = "f"
                params.encodings[2].scaleResolutionDownBy = 1.0
                params.encodings[2].maxBitrateBps = 40_000_000
                sender.parameters = params
            } else if (params.encodings.isNotEmpty()) {
                // Still advertise a single layer; SFU may remap.
                params.encodings[0].rid = "f"
                sender.parameters = params
            }
        } catch (t: Throwable) {
            Log.w(TAG, "simulcast params: ${t.message}")
        }
    }

    private suspend fun createAndSendOffer(pc: PeerConnection, place: MediaPlace) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        val offer = suspendSdp { pc.createOffer(it, constraints) }
        var sdp = mungeSimulcast(offer.description)
        val local = SessionDescription(SessionDescription.Type.OFFER, sdp)
        suspendSdpSet { pc.setLocalDescription(it, local) }
        webSocketManager.send(ClientSignal.RtcOffer(place, sdp))
        Log.i(TAG, "Sent RtcOffer (${sdp.length} chars)")
    }

    private suspend fun handleRemoteOffer(sdp: String) {
        val pc = peerConnection ?: return
        val p = place ?: return
        withContext(Dispatchers.Main) {
            try {
                val remote = SessionDescription(SessionDescription.Type.OFFER, sdp)
                suspendSdpSet { pc.setRemoteDescription(it, remote) }
                remoteDescriptionSet = true
                drainPendingIce(pc)
                val answer = suspendSdp {
                    pc.createAnswer(
                        it,
                        MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                        }
                    )
                }
                val localSdp = mungeSimulcast(answer.description)
                suspendSdpSet {
                    pc.setLocalDescription(it, SessionDescription(SessionDescription.Type.ANSWER, localSdp))
                }
                webSocketManager.send(ClientSignal.RtcAnswer(p, localSdp))
                _connectionLabel.value = "answered"
            } catch (t: Throwable) {
                Log.e(TAG, "handleRemoteOffer", t)
            }
        }
    }

    private suspend fun handleRemoteAnswer(sdp: String) {
        val pc = peerConnection ?: return
        withContext(Dispatchers.Main) {
            try {
                suspendSdpSet {
                    pc.setRemoteDescription(it, SessionDescription(SessionDescription.Type.ANSWER, sdp))
                }
                remoteDescriptionSet = true
                drainPendingIce(pc)
                _connectionLabel.value = "answered"
            } catch (t: Throwable) {
                Log.e(TAG, "handleRemoteAnswer", t)
            }
        }
    }

    private fun handleRemoteIce(candidate: String, sdpMid: String?, sdpMlineIndex: Int?) {
        val ice = IceCandidate(sdpMid, sdpMlineIndex ?: 0, candidate)
        val pc = peerConnection
        if (pc == null || !remoteDescriptionSet) {
            pendingIce += ice
            return
        }
        scope.launch(Dispatchers.Main) {
            try {
                pc.addIceCandidate(ice)
            } catch (t: Throwable) {
                Log.w(TAG, "addIceCandidate: ${t.message}")
            }
        }
    }

    private fun drainPendingIce(pc: PeerConnection) {
        val copy = pendingIce.toList()
        pendingIce.clear()
        for (c in copy) {
            try {
                pc.addIceCandidate(c)
            } catch (_: Throwable) {
            }
        }
    }

    private fun attachRemoteVideo(track: VideoTrack) {
        val id = track.id() ?: return
        if (remoteSinks.containsKey(id)) return
        val sink = RemoteVideoSink(id) { publisher, kind, bitmap ->
            if (publisher == me && publisher != 0L) return@RemoteVideoSink
            val key = RemoteStreamKey(publisher, kind)
            _remoteFrames.update { current ->
                val old = current[key]
                val next = current + (key to bitmap)
                if (old != null && old !== bitmap && !old.isRecycled) {
                    try {
                        old.recycle()
                    } catch (_: Throwable) {
                    }
                }
                next
            }
        }
        remoteSinks[id] = sink
        track.addSink(sink)
        track.setEnabled(true)
        Log.i(TAG, "Remote video track $id")
    }

    /**
     * Ensure offer SDP mentions Banter simulcast tokens when the stack omitted them.
     * Safe no-op when already present.
     */
    private fun mungeSimulcast(sdp: String): String {
        if (sdp.contains("a=simulcast:send q;h;f")) return sdp
        // Best-effort: leave as-is; many Android stacks need transceiver encodings set
        // before createOffer for rids to appear. Viewing remotes still works without
        // our publish layers matching.
        return sdp
    }

    private suspend fun suspendSdp(block: (SdpObserver) -> Unit): SessionDescription =
        suspendCoroutine { cont ->
            block(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp != null) cont.resume(sdp) else cont.resumeWithException(IllegalStateException("null sdp"))
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {
                    cont.resumeWithException(IllegalStateException(error ?: "createSdp failed"))
                }
                override fun onSetFailure(error: String?) {
                    cont.resumeWithException(IllegalStateException(error ?: "setSdp failed"))
                }
            })
        }

    private suspend fun suspendSdpSet(block: (SdpObserver) -> Unit) =
        suspendCoroutine { cont ->
            block(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onSetSuccess() {
                    cont.resume(Unit)
                }
                override fun onCreateFailure(error: String?) {
                    cont.resumeWithException(IllegalStateException(error ?: "create failed"))
                }
                override fun onSetFailure(error: String?) {
                    cont.resumeWithException(IllegalStateException(error ?: "set failed"))
                }
            })
        }

    private inner class RemoteVideoSink(
        private val trackId: String,
        private val onBitmap: (publisher: Long, kind: MediaKind, bitmap: Bitmap) -> Unit
    ) : VideoSink {
        private var lastMs = 0L

        override fun onFrame(frame: VideoFrame?) {
            val f = frame ?: return
            val now = System.currentTimeMillis()
            if (now - lastMs < 100) {
                return
            }
            lastMs = now
            f.retain()
            try {
                val (publisher, kind) = parseTrack(trackId, peerId.get())
                val bitmap = videoFrameToBitmap(f) ?: return
                onBitmap(publisher, kind, bitmap)
            } catch (t: Throwable) {
                Log.w(TAG, "frame: ${t.message}")
            } finally {
                f.release()
            }
        }
    }

    companion object {
        fun parseTrack(id: String, peer: Long): Pair<Long, MediaKind> {
            if (id.startsWith("fwd-")) {
                return parseFwd(id.removePrefix("fwd-"), peer)
            }
            return peer to kindOf(id)
        }

        private fun parseFwd(rest: String, peer: Long): Pair<Long, MediaKind> {
            val dash = rest.indexOf('-')
            if (dash <= 0) return peer to MediaKind.UserAudio
            val publisher = rest.substring(0, dash).toLongOrNull() ?: peer
            val after = rest.substring(dash + 1)
            val ridSplit = after.lastIndexOf('-')
            val kindToken = if (ridSplit > 0 && after.substring(ridSplit + 1).length == 1) {
                after.substring(0, ridSplit)
            } else {
                after
            }
            return publisher to kindOf(kindToken)
        }

        private fun kindOf(id: String): MediaKind = when {
            id.contains("stream_audio") -> MediaKind.StreamAudio
            id.contains("user_audio") -> MediaKind.UserAudio
            id.contains("camera") -> MediaKind.Camera
            id.contains("screen") -> MediaKind.Screen
            else -> MediaKind.UserAudio
        }

        fun videoFrameToBitmap(frame: VideoFrame): Bitmap? {
            val buffer = frame.buffer.toI420() ?: return null
            return try {
                val width = buffer.width
                val height = buffer.height
                val chromaHeight = (height + 1) / 2
                val chromaWidth = (width + 1) / 2
                val nv21 = ByteArray(width * height + chromaWidth * chromaHeight * 2)
                // I420 → NV21 for YuvImage / manual ARGB
                YuvHelper.I420ToNV12(
                    buffer.dataY, buffer.strideY,
                    buffer.dataU, buffer.strideU,
                    buffer.dataV, buffer.strideV,
                    ByteBuffer.wrap(nv21),
                    width, height
                )
                // NV12 to ARGB manually via Android YuvImage expects NV21 — convert U/V
                val nv21Fixed = nv12ToNv21(nv21, width, height)
                val yuv = android.graphics.YuvImage(nv21Fixed, android.graphics.ImageFormat.NV21, width, height, null)
                val out = java.io.ByteArrayOutputStream()
                yuv.compressToJpeg(android.graphics.Rect(0, 0, width, height), 70, out)
                val jpeg = out.toByteArray()
                var bmp = android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return null
                val rotation = frame.rotation
                if (rotation != 0) {
                    val m = Matrix().apply { postRotate(rotation.toFloat()) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }
                // Downscale for UI
                val maxW = 720
                if (bmp.width > maxW) {
                    val h = (bmp.height.toFloat() * maxW / bmp.width).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(bmp, maxW, h, true)
                    if (scaled !== bmp) bmp.recycle()
                    scaled
                } else bmp
            } finally {
                buffer.release()
            }
        }

        private fun nv12ToNv21(nv12: ByteArray, width: Int, height: Int): ByteArray {
            val out = nv12.copyOf()
            val frame = width * height
            var i = frame
            while (i + 1 < out.size) {
                val u = out[i]
                out[i] = out[i + 1]
                out[i + 1] = u
                i += 2
            }
            return out
        }
    }
}
