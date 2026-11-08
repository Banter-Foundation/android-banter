package com.banter.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("session")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val userId: Flow<Long?> = context.dataStore.data.map { it[USER_ID_KEY] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }
    val displayName: Flow<String?> = context.dataStore.data.map { it[DISPLAY_NAME_KEY] }
    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL_KEY] }

    suspend fun save(token: String, userId: Long, username: String, displayName: String) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[USER_ID_KEY] = userId
            it[USERNAME_KEY] = username
            it[DISPLAY_NAME_KEY] = displayName
        }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL_KEY] = url }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
