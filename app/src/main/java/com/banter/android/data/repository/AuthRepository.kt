package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.local.SessionStore
import com.banter.android.data.model.LoginRequest
import com.banter.android.data.model.RegisterRequest
import com.banter.android.data.model.Session
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: BanterApi,
    private val sessionStore: SessionStore
) {
    suspend fun login(username: String, password: String): Result<Session> = runCatching {
        val response = api.login(LoginRequest(username, password))
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Login failed")
        val session = response.body()!!
        sessionStore.save(session.token, session.account.id, session.account.username, session.account.displayName)
        session
    }

    suspend fun register(username: String, password: String): Result<Session> = runCatching {
        val response = api.register(RegisterRequest(username, password))
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Register failed")
        val session = response.body()!!
        sessionStore.save(session.token, session.account.id, session.account.username, session.account.displayName)
        session
    }

    suspend fun logout() {
        runCatching { api.logout() }
        sessionStore.clear()
    }
}
