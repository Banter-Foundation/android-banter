package com.banter.android.data.api

import com.banter.android.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface BanterApi {

    @POST("v1/register")
    suspend fun register(@Body request: RegisterRequest): Response<Session>

    @POST("v1/login")
    suspend fun login(@Body request: LoginRequest): Response<Session>

    @POST("v1/logout")
    suspend fun logout(): Response<Unit>

    @GET("v1/me")
    suspend fun me(): Response<Account>

    @GET("v1/version")
    suspend fun version(): Response<ServerVersion>

    // --- Friends ---

    @GET("v1/friends")
    suspend fun listFriends(): Response<List<Friend>>

    @GET("v1/friends/requests")
    suspend fun listFriendRequests(): Response<FriendRequestsResponse>

    @POST("v1/friends/requests")
    suspend fun sendFriendRequest(@Body body: FriendRequestInput): Response<FriendRequest>

    @POST("v1/friends/requests/{id}/accept")
    suspend fun acceptFriendRequest(@Path("id") id: Long): Response<Unit>

    @POST("v1/friends/requests/{id}/decline")
    suspend fun declineFriendRequest(@Path("id") id: Long): Response<Unit>

    @GET("v1/presence")
    suspend fun listPresence(): Response<List<PresenceEntry>>

    // --- Servers ---

    @GET("v1/servers")
    suspend fun listServers(): Response<List<ServerSummary>>

    @GET("v1/me/server-rail")
    suspend fun serverRail(): Response<ServerRail>

    @POST("v1/servers")
    suspend fun createServer(@Body body: CreateServerRequest): Response<ServerHome>

    /** Returns nested category layouts, not a flat ServerLayout. */
    @GET("v1/servers/{id}/layout")
    suspend fun serverLayout(@Path("id") id: Long): Response<List<CategoryLayout>>

    @GET("v1/servers/{id}/members")
    suspend fun members(@Path("id") id: Long): Response<List<Member>>

    @GET("v1/servers/{id}/roles")
    suspend fun listRoles(@Path("id") id: Long): Response<List<GuildRole>>

    @POST("v1/servers/{id}/channels")
    suspend fun createChannel(
        @Path("id") serverId: Long,
        @Body body: CreateChannelRequest
    ): Response<Channel>

    @POST("v1/servers/{id}/categories")
    suspend fun createCategory(
        @Path("id") serverId: Long,
        @Body body: CreateCategoryRequest
    ): Response<Category>

    @POST("v1/servers/{id}/invites")
    suspend fun createInvite(@Path("id") serverId: Long): Response<InviteResponse>

    @POST("v1/invites/{code}/join")
    suspend fun joinInvite(@Path("code") code: String): Response<ServerHome>

    @GET("v1/servers/{id}/voice")
    suspend fun serverVoice(@Path("id") id: Long): Response<List<VoiceRoster>>

    // --- Channels / Messages ---

    @GET("v1/channels/{id}/messages")
    suspend fun channelMessages(
        @Path("id") channelId: Long,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>

    @POST("v1/messages")
    suspend fun sendMessage(@Body body: SendMessageRequest): Response<Message>

    @PATCH("v1/messages/{id}")
    suspend fun editMessage(
        @Path("id") id: Long,
        @Body body: EditMessageRequest
    ): Response<Message>

    @DELETE("v1/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: Long): Response<Unit>

    // --- DMs ---

    @GET("v1/dms")
    suspend fun listDms(): Response<List<DirectChat>>

    @POST("v1/dms")
    suspend fun openDm(@Body body: OpenDmRequest): Response<DirectChat>

    @GET("v1/dms/{id}/messages")
    suspend fun dmMessages(
        @Path("id") dmId: Long,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<Message>>

    @POST("v1/dms/{id}/call")
    suspend fun startCall(@Path("id") dmId: Long): Response<Unit>

    @POST("v1/dms/{id}/call/accept")
    suspend fun acceptCall(@Path("id") dmId: Long): Response<Unit>

    @POST("v1/dms/{id}/call/decline")
    suspend fun declineCall(@Path("id") dmId: Long): Response<Unit>

    @POST("v1/dms/{id}/call/end")
    suspend fun endCall(@Path("id") dmId: Long): Response<Unit>

    // --- Voice ---

    @POST("v1/voice/join")
    suspend fun joinVoice(@Body body: JoinVoiceRequest): Response<Unit>

    @POST("v1/voice/leave")
    suspend fun leaveVoice(): Response<Unit>

    @POST("v1/voice/flags")
    suspend fun setVoiceFlags(@Body body: VoiceFlags): Response<Unit>

    /** STUN/TURN (+ SFU advertise hint) for WebRTC PeerConnections. */
    @GET("v1/ice")
    suspend fun ice(): Response<IceConfig>

    // --- Profile ---

    @GET("v1/users/{id}")
    suspend fun userProfile(
        @Path("id") userId: Long,
        @Query("server_id") serverId: Long? = null
    ): Response<UserProfile>

    @Multipart
    @POST("v1/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<Unit>

    @DELETE("v1/me/avatar")
    suspend fun deleteAvatar(): Response<Unit>

    @GET("v1/mentions")
    suspend fun mentions(): Response<List<Mention>>

    @POST("v1/mentions/{id}/read")
    suspend fun readMention(@Path("id") id: Long): Response<Unit>

    @GET("v1/emote-picker")
    suspend fun emotePicker(): Response<List<EmotePack>>
}
