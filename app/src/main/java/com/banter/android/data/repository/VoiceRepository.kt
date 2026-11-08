package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.JoinVoiceRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(private val api: BanterApi) {

    suspend fun join(channelId: Long): Result<Unit> = runCatching {
        val r = api.joinVoice(JoinVoiceRequest(channelId))
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun leave(): Result<Unit> = runCatching {
        val r = api.leaveVoice()
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }

    suspend fun setFlags(muted: Boolean, deafened: Boolean, camera: Boolean, screen: Boolean): Result<Unit> = runCatching {
        val r = api.setVoiceFlags(
            mapOf(
                "muted" to muted,
                "deafened" to deafened,
                "camera" to camera,
                "screen" to screen
            )
        )
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }
}
