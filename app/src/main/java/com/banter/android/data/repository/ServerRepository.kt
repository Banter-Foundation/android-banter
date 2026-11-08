package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(private val api: BanterApi) {

    suspend fun serverRail(): Result<ServerRail> = runCatching {
        val r = api.serverRail()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed to load server rail")
        r.body() ?: ServerRail()
    }

    /** Fallback flat list if rail fails. */
    suspend fun listServers(): Result<List<ServerSummary>> = runCatching {
        val r = api.listServers()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }

    suspend fun createServer(name: String): Result<ServerHome> = runCatching {
        val r = api.createServer(CreateServerRequest(name))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun serverLayout(serverId: Long): Result<List<CategoryLayout>> = runCatching {
        val r = api.serverLayout(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }

    suspend fun members(serverId: Long): Result<List<Member>> = runCatching {
        val r = api.members(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }

    suspend fun roles(serverId: Long): Result<List<GuildRole>> = runCatching {
        val r = api.listRoles(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }

    suspend fun createChannel(
        serverId: Long,
        name: String,
        kind: String = "text",
        categoryId: Long? = null
    ): Result<Channel> = runCatching {
        val r = api.createChannel(serverId, CreateChannelRequest(name, kind, categoryId))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun createCategory(serverId: Long, name: String): Result<Category> = runCatching {
        val r = api.createCategory(serverId, CreateCategoryRequest(name))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun createInvite(serverId: Long): Result<InviteResponse> = runCatching {
        val r = api.createInvite(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun joinInvite(code: String): Result<ServerHome> = runCatching {
        val r = api.joinInvite(code)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun serverVoice(serverId: Long): Result<List<VoiceRoster>> = runCatching {
        val r = api.serverVoice(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }
}
