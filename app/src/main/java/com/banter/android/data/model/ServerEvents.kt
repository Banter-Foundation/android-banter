package com.banter.android.data.model

import com.google.gson.JsonObject

/**
 * WebSocket events pushed by the server.
 * Deserialized by tag: { "kind": "...", ...fields }
 */
sealed class ServerEvent {
    data class Ready(val bot: Boolean, val user: Account) : ServerEvent()
    data class NewMessage(val message: Message) : ServerEvent()
    data class MessageEdited(
        val id: Long,
        val content: String,
        val editedAt: Long
    ) : ServerEvent()
    data class MessageDeleted(val id: Long) : ServerEvent()
    data class Presence(
        val userId: Long,
        val state: String,
        val customStatus: String?
    ) : ServerEvent()
    data class VoiceJoin(
        val userId: Long,
        val channelId: Long,
        val serverId: Long?
    ) : ServerEvent()
    data class VoiceLeave(
        val userId: Long,
        val channelId: Long?,
        val serverId: Long?
    ) : ServerEvent()
    data class VoiceFlags(
        val userId: Long,
        val muted: Boolean,
        val deafened: Boolean,
        val camera: Boolean,
        val screen: Boolean
    ) : ServerEvent()
    data class DmOpen(val dm: Dm) : ServerEvent()
    data class CallIncoming(
        val dmId: Long,
        val callerId: Long,
        val callerName: String
    ) : ServerEvent()
    data class CallEnded(val dmId: Long) : ServerEvent()
    data class FriendAdded(val friend: Friend) : ServerEvent()
    data class FriendRemoved(val userId: Long) : ServerEvent()
    data class FriendRequestReceived(val request: FriendRequest) : ServerEvent()
    data class RoleUpdated(val serverId: Long, val role: GuildRole) : ServerEvent()
    data class RoleDeleted(val serverId: Long, val roleId: Long) : ServerEvent()
    data class Unknown(val raw: JsonObject) : ServerEvent()
}

/** WebSocket signals sent from client → server. */
sealed class ClientSignal {
    data class Heartbeat(val at: Long) : ClientSignal()
    data class RtcOffer(val place: String, val sdp: String) : ClientSignal()
    data class RtcAnswer(val place: String, val sdp: String) : ClientSignal()
    data class RtcIce(val place: String, val candidate: String) : ClientSignal()
}
