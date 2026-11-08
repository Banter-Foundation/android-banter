package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(private val api: BanterApi) {

    suspend fun listFriends(): Result<List<Friend>> = runCatching {
        val r = api.listFriends()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }

    suspend fun listRequests(): Result<FriendRequestsResponse> = runCatching {
        val r = api.listFriendRequests()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: FriendRequestsResponse()
    }

    suspend fun sendRequest(username: String): Result<FriendRequest> = runCatching {
        val r = api.sendFriendRequest(FriendRequestInput(username))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body()!!
    }

    suspend fun acceptRequest(id: Long): Result<Unit> = runCatching {
        val r = api.acceptFriendRequest(id)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun declineRequest(id: Long): Result<Unit> = runCatching {
        val r = api.declineFriendRequest(id)
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun listPresence(): Result<List<PresenceEntry>> = runCatching {
        val r = api.listPresence()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
        r.body() ?: emptyList()
    }
}
