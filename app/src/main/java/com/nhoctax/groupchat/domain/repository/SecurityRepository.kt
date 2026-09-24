package com.nhoctax.groupchat.domain.repository

import com.nhoctax.groupchat.domain.model.SafetyNumber
import kotlinx.coroutines.flow.Flow
import java.security.KeyPair

interface SecurityRepository {
    suspend fun getOrCreateUserKeyPair(alias: String = DEFAULT_USER_ALIAS): KeyPair
    suspend fun getUserPublicKeyPem(alias: String = DEFAULT_USER_ALIAS): String
    suspend fun generateSafetyNumber(contactId: String): Result<SafetyNumber>
    fun getSafetyNumber(contactId: String): Flow<SafetyNumber?>
    suspend fun verifySafetyNumber(contactId: String, isVerified: Boolean): Result<Unit>

    companion object {
        const val DEFAULT_USER_ALIAS = "user_me_key"
        const val CURRENT_USER_ID = "user_me"
    }
}
