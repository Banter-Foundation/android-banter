package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DmRepository @Inject constructor(private val api: BanterApi) {

    suspend fun listDms(): Result<List<DirectChat>> = runCatching {
        val r = api.listDms()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }

    suspend fun openDm(userId: Long): Result<DirectChat> = runCatching {
        val r = api.openDm(OpenDmRequest(userId = userId))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun startCall(dmId: Long): Result<Unit> = runCatching {
        val r = api.startCall(dmId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun acceptCall(dmId: Long): Result<Unit> = runCatching {
        val r = api.acceptCall(dmId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun declineCall(dmId: Long): Result<Unit> = runCatching {
        val r = api.declineCall(dmId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun endCall(dmId: Long): Result<Unit> = runCatching {
        val r = api.endCall(dmId)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }
}
