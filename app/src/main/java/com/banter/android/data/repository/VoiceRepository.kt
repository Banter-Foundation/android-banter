package com.banter.android.data.repository

import com.banter.android.data.api.BanterApi
import com.banter.android.data.model.JoinVoiceRequest
import com.banter.android.data.model.VoiceFlags
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

    suspend fun setFlags(
        muted: Boolean,
        deafened: Boolean,
        camera: Boolean = false,
        screen: Boolean = false
    ): Result<Unit> = runCatching {
        val r = api.setVoiceFlags(
            VoiceFlags(
                muted = muted,
                deafened = deafened,
                camera = camera,
                screen = screen
            )
        )
        if (!r.isSuccessful) error(r.errorBody()?.string() ?: "Failed")
    }
}
