package com.example.composedemo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserManager(private val context: Context) {
    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PASSWORD = stringPreferencesKey("user_password")
        val LOGGED_IN_USER = stringPreferencesKey("logged_in_user")
    }

    suspend fun saveUser(name: String, email: String, pass: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = name
            prefs[USER_EMAIL] = email
            prefs[USER_PASSWORD] = pass
        }
    }

    suspend fun setLoggedInUser(email: String?) {
        context.dataStore.edit { prefs ->
            if (email == null) prefs.remove(LOGGED_IN_USER)
            else prefs[LOGGED_IN_USER] = email
        }
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val userPassword: Flow<String?> = context.dataStore.data.map { it[USER_PASSWORD] }
    val loggedInUser: Flow<String?> = context.dataStore.data.map { it[LOGGED_IN_USER] }
}
