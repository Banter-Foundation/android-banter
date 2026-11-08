package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(private val api: BanterApi) {

    suspend fun channelMessages(channelId: Long, before: Long? = null): Result<List<Message>> = runCatching {
        val r = api.channelMessages(channelId, before)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun dmMessages(dmId: Long, before: Long? = null): Result<List<Message>> = runCatching {
        val r = api.dmMessages(dmId, before)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun sendChannelMessage(channelId: Long, content: String, replyTo: Long? = null): Result<Message> = runCatching {
        val r = api.sendMessage(SendMessageRequest(content, channelId = channelId, replyTo = replyTo))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun sendDmMessage(dmId: Long, content: String, replyTo: Long? = null): Result<Message> = runCatching {
        val r = api.sendMessage(SendMessageRequest(content, dmId = dmId, replyTo = replyTo))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun editMessage(id: Long, content: String): Result<Message> = runCatching {
        val r = api.editMessage(id, mapOf("content" to content))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun deleteMessage(id: Long): Result<Unit> = runCatching {
        val r = api.deleteMessage(id)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }
}
