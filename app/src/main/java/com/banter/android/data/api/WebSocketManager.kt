package com.banter.android.data.api

import android.util.Log
import com.banter.android.data.model.*
import com.google.gson.Gson
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
        when (signal) {
            is ClientSignal.Heartbeat ->
                webSocket?.send("""{"kind":"heartbeat","at":${signal.at}}""")
        }
    }

    private fun parseTarget(obj: com.google.gson.JsonObject?): ChatTarget? {
        if (obj == null || !obj.has("type") || !obj.has("id")) return null
        return ChatTarget(obj.get("type").asString, obj.get("id").asLong)
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
                else -> ServerEvent.Unknown(obj)
            }
            _events.tryEmit(event)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse WS event: ${e.message}\nRaw: $text")
        }
    }
}

class OkHttp3Client(val client: OkHttpClient)
