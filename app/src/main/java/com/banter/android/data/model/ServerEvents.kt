package com.banter.android.data.model

import com.google.gson.JsonObject

/**
 * WebSocket events from Banter server (serde tag = "kind").
 */
sealed class ServerEvent {
    data class Ready(val bot: Boolean, val user: Account) : ServerEvent()
    data class MessageCreated(val target: ChatTarget, val message: Message) : ServerEvent()
    data class MessageEdited(val target: ChatTarget, val message: Message) : ServerEvent()
    data class MessageDeleted(val target: ChatTarget, val messageId: Long) : ServerEvent()
    data class Presence(
        val userId: Long,
        val state: String,
        val customStatus: String?
    ) : ServerEvent()
    data class Voice(val channelId: Long, val members: List<VoiceMember>) : ServerEvent()
    data class Call(val call: CallSnapshot) : ServerEvent()
    data class CallEnded(val dmId: Long) : ServerEvent()
    data class FriendRequestEvent(val request: FriendRequest) : ServerEvent()
    object FriendsChanged : ServerEvent()

    data class RtcOffer(val place: MediaPlace, val from: Long, val sdp: String) : ServerEvent()
    data class RtcAnswer(val place: MediaPlace, val from: Long, val sdp: String) : ServerEvent()
    data class RtcIce(
        val place: MediaPlace,
        val from: Long,
        val candidate: String,
        val sdpMid: String?,
        val sdpMlineIndex: Int?
    ) : ServerEvent()

    data class SubscribeLayer(
        val place: MediaPlace,
        val subscriber: Long,
        val publisher: Long,
        val track: MediaKind,
        val maxLayer: LayerId
    ) : ServerEvent()

    data class WantedLayers(
        val place: MediaPlace,
        val track: MediaKind,
        val layers: List<LayerId>,
        val subscribers: Long
    ) : ServerEvent()

    data class Unknown(val raw: JsonObject) : ServerEvent()
}

/**
 * Client → server websocket signals (serde tag = "kind").
 */
sealed class ClientSignal {
    object Activity : ClientSignal()

    data class RtcOffer(val place: MediaPlace, val sdp: String) : ClientSignal()
    data class RtcAnswer(val place: MediaPlace, val sdp: String) : ClientSignal()
    data class RtcIce(
        val place: MediaPlace,
        val candidate: String,
        val sdpMid: String?,
        val sdpMlineIndex: Int?
    ) : ClientSignal()

    data class SubscribeLayer(
        val place: MediaPlace,
        val publisher: Long,
        val track: MediaKind,
        val maxLayer: LayerId
    ) : ClientSignal()

    data class GuildP2pVideo(val enabled: Boolean) : ClientSignal()
}
