package com.banter.android.data.api

import android.util.Log
import com.banter.android.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttp3Client: OkHttp3Client,
    private val gson: Gson
) {
    private val TAG = "WebSocketManager"
    private var webSocket: WebSocket? = null

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerEvent> = _events

    private val _connectionState = MutableSharedFlow<ConnectionState>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }

    fun connect(baseUrl: String, token: String) {
        disconnect()
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/v1/ws"

        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $token")
            .build()

        _connectionState.tryEmit(ConnectionState.CONNECTING)

        webSocket = okHttp3Client.client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected")
                _connectionState.tryEmit(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndEmit(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed: $code $reason")
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.tryEmit(ConnectionState.DISCONNECTED)
    }

    fun send(signal: ClientSignal) {
        val json = when (signal) {
            ClientSignal.Activity -> """{"kind":"activity"}"""
            is ClientSignal.RtcOffer -> JsonObject().apply {
                addProperty("kind", "rtc_offer")
                add("place", signal.place.toJsonObject())
                addProperty("sdp", signal.sdp)
            }.toString()
            is ClientSignal.RtcAnswer -> JsonObject().apply {
                addProperty("kind", "rtc_answer")
                add("place", signal.place.toJsonObject())
                addProperty("sdp", signal.sdp)
            }.toString()
            is ClientSignal.RtcIce -> JsonObject().apply {
                addProperty("kind", "rtc_ice")
                add("place", signal.place.toJsonObject())
                addProperty("candidate", signal.candidate)
                if (signal.sdpMid != null) addProperty("sdp_mid", signal.sdpMid)
                if (signal.sdpMlineIndex != null) addProperty("sdp_mline_index", signal.sdpMlineIndex)
            }.toString()
            is ClientSignal.SubscribeLayer -> JsonObject().apply {
                addProperty("kind", "subscribe_layer")
                add("place", signal.place.toJsonObject())
                addProperty("publisher", signal.publisher)
                addProperty("track", signal.track.wire)
                addProperty("max_layer", signal.maxLayer.wire)
            }.toString()
            is ClientSignal.GuildP2pVideo -> JsonObject().apply {
                addProperty("kind", "guild_p2p_video")
                addProperty("enabled", signal.enabled)
            }.toString()
        }
        val ok = webSocket?.send(json) == true
        if (!ok) Log.w(TAG, "Failed to send signal ${signal::class.simpleName}")
    }

    private fun parseTarget(obj: JsonObject?): ChatTarget? {
        if (obj == null || !obj.has("type") || !obj.has("id")) return null
        return ChatTarget(obj.get("type").asString, obj.get("id").asLong)
    }

    private fun parseLayers(arr: JsonArray?): List<LayerId> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            if (el.isJsonPrimitive) LayerId.fromWire(el.asString) else null
        }
    }

    private fun parseAndEmit(text: String) {
        try {
            val obj = JsonParser.parseString(text).asJsonObject
            val kind = obj.get("kind")?.asString ?: return
            val event = when (kind) {
                "ready" -> ServerEvent.Ready(
                    bot = obj.get("bot")?.asBoolean ?: false,
                    user = gson.fromJson(obj.get("user"), Account::class.java)
                )
                "message_created" -> {
                    val target = parseTarget(obj.getAsJsonObject("target")) ?: return
                    ServerEvent.MessageCreated(
                        target = target,
                        message = gson.fromJson(obj.get("message"), Message::class.java)
                    )
                }
                "message_edited" -> {
                    val target = parseTarget(obj.getAsJsonObject("target")) ?: return
                    ServerEvent.MessageEdited(
                        target = target,
                        message = gson.fromJson(obj.get("message"), Message::class.java)
                    )
                }
                "message_deleted" -> {
                    val target = parseTarget(obj.getAsJsonObject("target")) ?: return
                    val messageId = obj.get("message_id")?.asLong ?: return
                    ServerEvent.MessageDeleted(target = target, messageId = messageId)
                }
                "presence" -> ServerEvent.Presence(
                    userId = obj.get("user_id").asLong,
                    state = obj.get("state").asString,
                    customStatus = obj.get("custom_status")?.takeIf { !it.isJsonNull }?.asString
                )
                "voice" -> ServerEvent.Voice(
                    channelId = obj.get("channel_id").asLong,
                    members = gson.fromJson(
                        obj.get("members"),
                        Array<VoiceMember>::class.java
                    ).toList()
                )
                "call" -> {
                    val callObj = obj.get("call") ?: return
                    ServerEvent.Call(call = gson.fromJson(callObj, CallSnapshot::class.java))
                }
                "call_ended" -> ServerEvent.CallEnded(dmId = obj.get("dm_id").asLong)
                "friend_request" -> ServerEvent.FriendRequestEvent(
                    request = gson.fromJson(obj.get("request"), FriendRequest::class.java)
                )
                "friends_changed" -> ServerEvent.FriendsChanged
                "rtc_offer" -> {
                    val place = MediaPlace.fromJson(obj.getAsJsonObject("place")) ?: return
                    ServerEvent.RtcOffer(
                        place = place,
                        from = obj.get("from")?.asLong ?: 0L,
                        sdp = obj.get("sdp")?.asString ?: return
                    )
                }
                "rtc_answer" -> {
                    val place = MediaPlace.fromJson(obj.getAsJsonObject("place")) ?: return
                    ServerEvent.RtcAnswer(
                        place = place,
                        from = obj.get("from")?.asLong ?: 0L,
                        sdp = obj.get("sdp")?.asString ?: return
                    )
                }
                "rtc_ice" -> {
                    val place = MediaPlace.fromJson(obj.getAsJsonObject("place")) ?: return
                    ServerEvent.RtcIce(
                        place = place,
                        from = obj.get("from")?.asLong ?: 0L,
                        candidate = obj.get("candidate")?.asString ?: return,
                        sdpMid = obj.get("sdp_mid")?.takeIf { !it.isJsonNull }?.asString,
                        sdpMlineIndex = obj.get("sdp_mline_index")?.takeIf { !it.isJsonNull }?.asInt
                    )
                }
                "subscribe_layer" -> {
                    val place = MediaPlace.fromJson(obj.getAsJsonObject("place")) ?: return
                    ServerEvent.SubscribeLayer(
                        place = place,
                        subscriber = obj.get("subscriber")?.asLong ?: 0L,
                        publisher = obj.get("publisher")?.asLong ?: 0L,
                        track = MediaKind.fromWire(obj.get("track")?.asString),
                        maxLayer = LayerId.fromWire(obj.get("max_layer")?.asString)
                    )
                }
                "wanted_layers" -> {
                    val place = MediaPlace.fromJson(obj.getAsJsonObject("place")) ?: return
                    ServerEvent.WantedLayers(
                        place = place,
                        track = MediaKind.fromWire(obj.get("track")?.asString),
                        layers = parseLayers(obj.getAsJsonArray("layers")),
                        subscribers = obj.get("subscribers")?.asLong ?: 0L
                    )
                }
                else -> ServerEvent.Unknown(obj)
            }
            _events.tryEmit(event)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse WS event: ${e.message}\nRaw: $text")
        }
    }
}

class OkHttp3Client(val client: OkHttpClient)
