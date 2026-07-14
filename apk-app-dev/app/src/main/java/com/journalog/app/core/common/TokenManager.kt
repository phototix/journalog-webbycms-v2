package com.journalog.app.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        private val ROLE_ID_KEY = intPreferencesKey("role_id")
        private val SHOW_DEBUG_KEY = booleanPreferencesKey("show_debug")
    }

    val showDebugFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SHOW_DEBUG_KEY] ?: false }

    suspend fun setShowDebug(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_DEBUG_KEY] = show }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[USER_ID_KEY] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[USERNAME_KEY] }
    val nameFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[NAME_KEY] }
    val avatarFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[AVATAR_KEY] }
    val roleIdFlow: Flow<Int> = context.dataStore.data.map { prefs -> prefs[ROLE_ID_KEY] ?: 2 }
    val isAdminFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> (prefs[ROLE_ID_KEY] ?: 2) == 1 }

    suspend fun saveSession(token: String, userId: Int, username: String, name: String, avatar: String, roleId: Int = 2) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId.toString()
            prefs[USERNAME_KEY] = username
            prefs[NAME_KEY] = name
            prefs[AVATAR_KEY] = avatar
            prefs[ROLE_ID_KEY] = roleId
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun getRoleId(): Int {
        return context.dataStore.data.first()[ROLE_ID_KEY] ?: 2
    }

    suspend fun isAdmin(): Boolean {
        return getRoleId() == 1
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
