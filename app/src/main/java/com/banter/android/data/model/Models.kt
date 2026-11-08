package com.banter.android.data.model

import com.google.gson.annotations.SerializedName

data class Account(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean = false,
    @SerializedName("avatar_rev") val avatarRev: Long = 0,
    val bot: Boolean = false
)

data class Session(
    val token: String,
    val account: Account
)

data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val password: String)

data class Friend(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean = false,
    @SerializedName("avatar_rev") val avatarRev: Long = 0
)

/** Matches server FriendRequest (flat ids/usernames, not nested Account). */
data class FriendRequest(
    val id: Long,
    @SerializedName("from_id") val fromId: Long,
    @SerializedName("from_username") val fromUsername: String,
    @SerializedName("to_id") val toId: Long,
    @SerializedName("to_username") val toUsername: String
)

data class FriendRequestsResponse(
    val incoming: List<FriendRequest> = emptyList(),
    val outgoing: List<FriendRequest> = emptyList()
)

data class ServerSummary(
    val id: Long,
    val name: String,
    @SerializedName("owner_id") val ownerId: Long
)

data class ServerFolder(
    val id: Long,
    @SerializedName("parent_id") val parentId: Long? = null,
    val name: String,
    val color: Long = 0,
    val position: Long = 0,
    val collapsed: Boolean = false
)

data class ServerRailServer(
    val server: ServerSummary,
    @SerializedName("folder_id") val folderId: Long? = null,
    val position: Long = 0
)

/** GET /v1/me/server-rail */
data class ServerRail(
    val folders: List<ServerFolder> = emptyList(),
    val servers: List<ServerRailServer> = emptyList()
)

data class Category(
    val id: Long,
    @SerializedName("server_id") val serverId: Long,
    val name: String,
    val position: Long = 0
)

data class Channel(
    val id: Long,
    @SerializedName("server_id") val serverId: Long,
    @SerializedName("category_id") val categoryId: Long,
    val name: String,
    val kind: String, // "text", "voice", "board", or plugin kind
    val position: Long = 0,
    val icon: String? = null
)

data class CategoryLayout(
    val category: Category,
    val channels: List<Channel> = emptyList()
)

/** GET /v1/servers/{id}/layout returns List<CategoryLayout> */
data class ServerHome(
    val server: ServerSummary,
    val categories: List<CategoryLayout> = emptyList()
)

data class Attachment(
    val id: Long,
    val filename: String,
    @SerializedName("byte_size") val byteSize: Long
)

data class Message(
    val id: Long,
    @SerializedName("author_id") val authorId: Long,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("author_display_name") val authorDisplayName: String,
    @SerializedName("author_nickname") val authorNickname: String? = null,
    val body: String,
    @SerializedName("reply_to") val replyTo: Long? = null,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("edited_at") val editedAt: Long? = null,
    val attachments: List<Attachment> = emptyList(),
    @SerializedName("author_kind") val authorKind: String = "user",
    @SerializedName("plugin_id") val pluginId: String? = null
) {
    val displayAuthor: String
        get() = authorNickname?.takeIf { it.isNotBlank() } ?: authorDisplayName
}

/**
 * ChatTarget wire form: { "type": "channel"|"direct", "id": N }
 * (serde tag=type, content=id)
 */
data class ChatTarget(
    val type: String,
    val id: Long
) {
    companion object {
        fun channel(id: Long) = ChatTarget("channel", id)
        fun direct(id: Long) = ChatTarget("direct", id)
    }
}

data class SendMessageRequest(
    val target: ChatTarget,
    val body: String,
    @SerializedName("reply_to") val replyTo: Long? = null
)

data class EditMessageRequest(val body: String)

/** GET /v1/dms — DirectChat has no last_message field. */
data class DirectChat(
    val id: Long,
    @SerializedName("peer_id") val peerId: Long,
    @SerializedName("peer_username") val peerUsername: String,
    @SerializedName("peer_display_name") val peerDisplayName: String,
    @SerializedName("peer_has_avatar") val peerHasAvatar: Boolean = false,
    @SerializedName("peer_avatar_rev") val peerAvatarRev: Long = 0
)

data class Member(
    @SerializedName("user_id") val userId: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    val nickname: String? = null,
    @SerializedName("is_owner") val isOwner: Boolean = false,
    @SerializedName("has_avatar") val hasAvatar: Boolean = false,
    @SerializedName("avatar_rev") val avatarRev: Long = 0,
    val bot: Boolean = false,
    val roles: List<GuildRole> = emptyList(),
    val presence: String? = null,
    @SerializedName("custom_status") val customStatus: String? = null
)

data class GuildRole(
    val id: Long,
    @SerializedName("server_id") val serverId: Long,
    val name: String,
    val color: Long = 0,
    val position: Long = 0,
    val hoist: Boolean = false,
    val permissions: Long = 0,
    val everyone: Boolean = false
)

data class VoiceFlags(
    val muted: Boolean = false,
    val deafened: Boolean = false,
    @SerializedName("push_to_talk") val pushToTalk: Boolean = false,
    val transmitting: Boolean = false,
    val camera: Boolean = false,
    val screen: Boolean = false
)

data class VoiceMember(
    @SerializedName("user_id") val userId: Long,
    val flags: VoiceFlags = VoiceFlags(),
    @SerializedName("prefer_p2p_video") val preferP2pVideo: Boolean = false
)

data class VoiceRoster(
    @SerializedName("channel_id") val channelId: Long,
    val members: List<VoiceMember> = emptyList()
)

data class CallSnapshot(
    @SerializedName("dm_id") val dmId: Long,
    val phase: String, // "ringing" | "active"
    val caller: Long,
    val callee: Long,
    @SerializedName("caller_flags") val callerFlags: VoiceFlags = VoiceFlags(),
    @SerializedName("callee_flags") val calleeFlags: VoiceFlags = VoiceFlags()
)

data class PresenceEntry(
    @SerializedName("user_id") val userId: Long,
    val state: String, // "online", "idle", "offline"
    @SerializedName("custom_status") val customStatus: String? = null
)

data class UserProfile(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean = false,
    @SerializedName("avatar_rev") val avatarRev: Long = 0,
    val nickname: String? = null,
    @SerializedName("is_self") val isSelf: Boolean = false,
    @SerializedName("is_friend") val isFriend: Boolean = false,
    @SerializedName("about_me") val aboutMe: String? = null,
    val pronouns: String? = null,
    @SerializedName("custom_status") val customStatus: String? = null,
    @SerializedName("member_since") val memberSince: Long = 0,
    @SerializedName("has_banner") val hasBanner: Boolean = false,
    @SerializedName("banner_rev") val bannerRev: Long = 0
)

data class Mention(
    val id: Long,
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("channel_id") val channelId: Long? = null,
    @SerializedName("server_id") val serverId: Long? = null,
    val read: Boolean = false
)

data class CreateServerRequest(val name: String)
data class CreateChannelRequest(
    val name: String,
    val kind: String = "text",
    @SerializedName("category_id") val categoryId: Long? = null
)
data class CreateCategoryRequest(val name: String)
data class InviteResponse(val code: String)
data class JoinVoiceRequest(@SerializedName("channel_id") val channelId: Long)
data class FriendRequestInput(val username: String)
data class OpenDmRequest(
    @SerializedName("user_id") val userId: Long? = null,
    val username: String? = null
)
data class ServerVersion(
    val version: String = "",
    val protocol: Int = 0
)

data class EmotePack(
    val id: Long,
    val name: String,
    val emotes: List<Emote> = emptyList()
)

data class Emote(
    val id: Long,
    val name: String,
    val kind: String = "emote"
)

/** `GET /v1/ice` — STUN/TURN for PeerConnections (off-LAN needs TURN). */
data class IceConfig(
    val stun: String = "stun:stun.l.google.com:19302",
    val turn: IceTurn? = null,
    @SerializedName("advertise_ip") val advertiseIp: String = "",
    @SerializedName("udp_min") val udpMin: Long? = null,
    @SerializedName("udp_max") val udpMax: Long? = null
)

data class IceTurn(
    val urls: String = "",
    val username: String = "",
    val credential: String = ""
)

/** Banter media place — serde externally-tagged newtype (`{"VoiceChannel":9}`). */
sealed class MediaPlace {
    data class VoiceChannel(val id: Long) : MediaPlace()
    data class DirectCall(val id: Long) : MediaPlace()

    fun toJsonObject(): com.google.gson.JsonObject {
        val o = com.google.gson.JsonObject()
        when (this) {
            is VoiceChannel -> o.addProperty("VoiceChannel", id)
            is DirectCall -> o.addProperty("DirectCall", id)
        }
        return o
    }

    companion object {
        fun fromJson(obj: com.google.gson.JsonObject?): MediaPlace? {
            if (obj == null) return null
            if (obj.has("VoiceChannel")) return VoiceChannel(obj.get("VoiceChannel").asLong)
            if (obj.has("DirectCall")) return DirectCall(obj.get("DirectCall").asLong)
            return null
        }
    }
}

enum class MediaKind(val wire: String) {
    UserAudio("user_audio"),
    StreamAudio("stream_audio"),
    Camera("camera"),
    Screen("screen");

    companion object {
        fun fromWire(value: String?): MediaKind = when (value) {
            "stream_audio" -> StreamAudio
            "camera" -> Camera
            "screen" -> Screen
            else -> UserAudio
        }
    }
}

enum class LayerId(val wire: String) {
    Full("full"),
    Half("half"),
    Quarter("quarter"),
    Off("off");

    companion object {
        fun fromWire(value: String?): LayerId = when (value) {
            "half" -> Half
            "quarter" -> Quarter
            "off" -> Off
            else -> Full
        }
    }
}
