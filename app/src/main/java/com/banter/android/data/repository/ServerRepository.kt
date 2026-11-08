package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(private val api: BanterApi) {

    suspend fun serverRail(): Result<List<ServerRailEntry>> = runCatching {
        val r = api.serverRail()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun listServers(): Result<List<Server>> = runCatching {
        val r = api.listServers()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun createServer(name: String): Result<Server> = runCatching {
        val r = api.createServer(CreateServerRequest(name))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun serverLayout(serverId: Long): Result<ServerLayout> = runCatching {
        val r = api.serverLayout(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun members(serverId: Long): Result<List<Member>> = runCatching {
        val r = api.members(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun roles(serverId: Long): Result<List<GuildRole>> = runCatching {
        val r = api.listRoles(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun createChannel(serverId: Long, name: String, kind: String = "text", categoryId: Long? = null): Result<Channel> = runCatching {
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

    suspend fun joinInvite(code: String): Result<Server> = runCatching {
        val r = api.joinInvite(code)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun serverVoice(serverId: Long): Result<List<VoiceMember>> = runCatching {
        val r = api.serverVoice(serverId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }
}
