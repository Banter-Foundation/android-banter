package com.banter.android.data.api

import android.util.Log
import com.banter.android.data.model.*
import com.google.gson.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttp3Client
) {
    private val TAG = "WebSocketManager"
    private var webSocket: WebSocket? = null
    private val gson = Gson()

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

        webSocket = okHttpClient.client.newWebSocket(request, object : WebSocketListener() {
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
            is ClientSignal.Heartbeat ->
                """{"kind":"heartbeat","at":${signal.at}}"""
            is ClientSignal.RtcOffer ->
                gson.toJson(mapOf("kind" to "rtc_offer", "place" to signal.place, "sdp" to signal.sdp))
            is ClientSignal.RtcAnswer ->
                gson.toJson(mapOf("kind" to "rtc_answer", "place" to signal.place, "sdp" to signal.sdp))
            is ClientSignal.RtcIce ->
                gson.toJson(mapOf("kind" to "rtc_ice", "place" to signal.place, "candidate" to signal.candidate))
        }
        webSocket?.send(json)
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
                "new_message" -> ServerEvent.NewMessage(
                    message = gson.fromJson(obj.get("message"), Message::class.java)
                )
                "message_edited" -> ServerEvent.MessageEdited(
                    id = obj.get("id").asLong,
                    content = obj.get("content").asString,
                    editedAt = obj.get("edited_at").asLong
                )
                "message_deleted" -> ServerEvent.MessageDeleted(
                    id = obj.get("id").asLong
                )
                "presence" -> ServerEvent.Presence(
                    userId = obj.get("user_id").asLong,
                    state = obj.get("state").asString,
                    customStatus = obj.get("custom_status")?.asString
                )
                "voice_join" -> ServerEvent.VoiceJoin(
                    userId = obj.get("user_id").asLong,
                    channelId = obj.get("channel_id").asLong,
                    serverId = obj.get("server_id")?.asLong
                )
                "voice_leave" -> ServerEvent.VoiceLeave(
                    userId = obj.get("user_id").asLong,
                    channelId = obj.get("channel_id")?.asLong,
                    serverId = obj.get("server_id")?.asLong
                )
                "voice_flags" -> ServerEvent.VoiceFlags(
                    userId = obj.get("user_id").asLong,
                    muted = obj.get("muted")?.asBoolean ?: false,
                    deafened = obj.get("deafened")?.asBoolean ?: false,
                    camera = obj.get("camera")?.asBoolean ?: false,
                    screen = obj.get("screen")?.asBoolean ?: false
                )
                "dm_open" -> ServerEvent.DmOpen(
                    dm = gson.fromJson(obj.get("dm"), Dm::class.java)
                )
                "call_incoming" -> ServerEvent.CallIncoming(
                    dmId = obj.get("dm_id").asLong,
                    callerId = obj.get("caller_id").asLong,
                    callerName = obj.get("caller_name").asString
                )
                "call_ended" -> ServerEvent.CallEnded(
                    dmId = obj.get("dm_id").asLong
                )
                "friend_added" -> ServerEvent.FriendAdded(
                    friend = gson.fromJson(obj.get("friend"), Friend::class.java)
                )
                "friend_removed" -> ServerEvent.FriendRemoved(
                    userId = obj.get("user_id").asLong
                )
                "friend_request" -> ServerEvent.FriendRequestReceived(
                    request = gson.fromJson(obj.get("request"), FriendRequest::class.java)
                )
                else -> ServerEvent.Unknown(obj)
            }
            _events.tryEmit(event)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse WS event: ${e.message}\nRaw: $text")
        }
    }
}

/** Wrapper so DI can inject OkHttpClient without namespace collision. */
class OkHttp3Client(val client: OkHttpClient)
