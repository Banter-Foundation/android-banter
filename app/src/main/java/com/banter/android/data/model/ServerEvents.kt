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
    data class Unknown(val raw: JsonObject) : ServerEvent()
}

sealed class ClientSignal {
    data class Heartbeat(val at: Long) : ClientSignal()
}
