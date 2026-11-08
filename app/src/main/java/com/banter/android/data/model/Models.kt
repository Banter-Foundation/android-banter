package com.banter.android.data.model

import com.google.gson.annotations.SerializedName

data class Account(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean,
    @SerializedName("avatar_rev") val avatarRev: Long,
    val bot: Boolean = false
)

data class Session(
    val token: String,
    val account: Account
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String
)

data class Friend(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean,
    @SerializedName("avatar_rev") val avatarRev: Long
)

data class FriendRequest(
    val id: Long,
    val from: Account,
    val to: Account
)

data class Server(
    val id: Long,
    val name: String,
    @SerializedName("has_icon") val hasIcon: Boolean,
    @SerializedName("icon_rev") val iconRev: Long,
    @SerializedName("owner_id") val ownerId: Long
)

data class ServerFolder(
    val id: Long,
    val name: String,
    @SerializedName("server_ids") val serverIds: List<Long>
)

data class ServerRailEntry(
    val id: Long,
    val name: String,
    @SerializedName("has_icon") val hasIcon: Boolean,
    @SerializedName("icon_rev") val iconRev: Long,
    @SerializedName("folder_id") val folderId: Long?
)

data class Channel(
    val id: Long,
    @SerializedName("server_id") val serverId: Long,
    @SerializedName("category_id") val categoryId: Long?,
    val name: String,
    val kind: String, // "text", "voice", "board"
    val position: Int,
    @SerializedName("has_icon") val hasIcon: Boolean = false,
    @SerializedName("icon_rev") val iconRev: Long = 0
)

data class Category(
    val id: Long,
    @SerializedName("server_id") val serverId: Long,
    val name: String,
    val position: Int
)

data class ServerLayout(
    val categories: List<Category>,
    val channels: List<Channel>
)

data class Message(
    val id: Long,
    @SerializedName("channel_id") val channelId: Long?,
    @SerializedName("dm_id") val dmId: Long?,
    @SerializedName("author_id") val authorId: Long,
    val author: Account,
    val content: String,
    @SerializedName("sent_at") val sentAt: Long,
    @SerializedName("edited_at") val editedAt: Long?,
    val attachments: List<Attachment> = emptyList(),
    @SerializedName("reply_to") val replyTo: Message? = null
)

data class Attachment(
    val id: Long,
    val filename: String,
    @SerializedName("byte_size") val byteSize: Long,
    @SerializedName("content_type") val contentType: String,
    val width: Int?,
    val height: Int?
)

data class Dm(
    val id: Long,
    @SerializedName("peer_id") val peerId: Long,
    @SerializedName("peer_username") val peerUsername: String,
    @SerializedName("peer_display_name") val peerDisplayName: String,
    @SerializedName("peer_has_avatar") val peerHasAvatar: Boolean,
    @SerializedName("peer_avatar_rev") val peerAvatarRev: Long,
    @SerializedName("last_message") val lastMessage: Message?,
    @SerializedName("call_state") val callState: String? // "idle", "ringing", "active"
)

data class Member(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean,
    @SerializedName("avatar_rev") val avatarRev: Long,
    val nickname: String?,
    val roles: List<Long> = emptyList()
)

data class GuildRole(
    val id: Long,
    @SerializedName("server_id") val serverId: Long,
    val name: String,
    val color: Long,
    val position: Int,
    @SerializedName("is_owner") val isOwner: Boolean = false
)

data class VoiceMember(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("channel_id") val channelId: Long,
    val muted: Boolean,
    val deafened: Boolean,
    val camera: Boolean,
    val screen: Boolean
)

data class VoiceState(
    @SerializedName("channel_id") val channelId: Long?,
    val members: List<VoiceMember>
)

data class PresenceEntry(
    @SerializedName("user_id") val userId: Long,
    val state: String, // "online", "idle", "dnd", "offline"
    @SerializedName("custom_status") val customStatus: String?
)

data class UserProfile(
    val id: Long,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("has_avatar") val hasAvatar: Boolean,
    @SerializedName("avatar_rev") val avatarRev: Long,
    val nickname: String?,
    @SerializedName("is_self") val isSelf: Boolean,
    @SerializedName("is_friend") val isFriend: Boolean,
    @SerializedName("about_me") val aboutMe: String?,
    val pronouns: String?,
    @SerializedName("custom_status") val customStatus: String?,
    @SerializedName("member_since") val memberSince: Long,
    @SerializedName("has_banner") val hasBanner: Boolean,
    @SerializedName("banner_rev") val bannerRev: Long,
    @SerializedName("theme_primary") val themePrimary: Long,
    @SerializedName("theme_secondary") val themeSecondary: Long
)

data class Mention(
    val id: Long,
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("channel_id") val channelId: Long?,
    @SerializedName("server_id") val serverId: Long?,
    val read: Boolean,
    val message: Message?
)

data class SendMessageRequest(
    val content: String,
    @SerializedName("channel_id") val channelId: Long? = null,
    @SerializedName("dm_id") val dmId: Long? = null,
    @SerializedName("reply_to") val replyTo: Long? = null
)

data class CreateServerRequest(val name: String)

data class CreateChannelRequest(
    val name: String,
    val kind: String = "text",
    @SerializedName("category_id") val categoryId: Long? = null
)

data class CreateCategoryRequest(val name: String)

data class InviteResponse(val code: String)

data class JoinVoiceRequest(
    @SerializedName("channel_id") val channelId: Long
)

data class FriendRequestInput(val username: String)

data class ServerVersion(
    val version: String,
    val protocol: Int
)
