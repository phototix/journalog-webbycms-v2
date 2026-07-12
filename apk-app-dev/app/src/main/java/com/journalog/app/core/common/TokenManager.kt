package com.journalog.app.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "journalog_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val NAME_KEY = stringPreferencesKey("name")
        private val AVATAR_KEY = stringPreferencesKey("avatar")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USERNAME_KEY]
    }

    val nameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[NAME_KEY]
    }

    val avatarFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[AVATAR_KEY]
    }

    suspend fun saveSession(token: String, userId: Int, username: String, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId.toString()
            prefs[USERNAME_KEY] = username
            prefs[NAME_KEY] = name
            prefs[AVATAR_KEY] = avatar
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
