package com.nhoctax.groupchat.data.repository

import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.crypto.SafetyNumberGenerator
import com.nhoctax.groupchat.domain.model.SafetyNumber
import com.nhoctax.groupchat.domain.repository.ContactRepository
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyPair

class SecurityRepositoryImpl(
    private val keyStoreManager: KeyStoreManager,
    private val safetyNumberGenerator: SafetyNumberGenerator,
    private val contactRepository: ContactRepository,
    private val userAlias: String = SecurityRepository.DEFAULT_USER_ALIAS,
    private val userId: String = SecurityRepository.CURRENT_USER_ID
) : SecurityRepository {

    override suspend fun getOrCreateUserKeyPair(alias: String): KeyPair {
        return keyStoreManager.getOrCreateKeyPair(alias)
    }

    override suspend fun getUserPublicKeyPem(alias: String): String {
        val keyPair = getOrCreateUserKeyPair(alias)
        return keyStoreManager.exportPublicKeyPem(keyPair.public)
    }

    override suspend fun generateSafetyNumber(contactId: String): Result<SafetyNumber> {
        return try {
            val userKeyPair = getOrCreateUserKeyPair(userAlias)
            val contact = contactRepository.getContact(contactId)
                ?: return Result.failure(IllegalArgumentException("Contact with ID $contactId not found"))

            val contactPublicKeyPem = contact.publicKeyPem
                ?: return Result.failure(IllegalStateException("Contact public key is missing"))

            val contactPublicKey = keyStoreManager.parsePublicKeyPem(contactPublicKeyPem)

            val safetyNumber = safetyNumberGenerator.generateSafetyNumber(
                user1Id = userId,
                publicKey1 = userKeyPair.public,
                user2Id = contactId,
                publicKey2 = contactPublicKey
            ).copy(isVerified = contact.isVerified)

            Result.success(safetyNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSafetyNumber(contactId: String): Flow<SafetyNumber?> {
        return contactRepository.getContactById(contactId).map { contact ->
            if (contact?.publicKeyPem == null) return@map null
            try {
                val userKeyPair = keyStoreManager.getOrCreateKeyPair(userAlias)
                val contactPublicKey = keyStoreManager.parsePublicKeyPem(contact.publicKeyPem)
                safetyNumberGenerator.generateSafetyNumber(
                    user1Id = userId,
                    publicKey1 = userKeyPair.public,
                    user2Id = contactId,
                    publicKey2 = contactPublicKey
                ).copy(isVerified = contact.isVerified)
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun verifySafetyNumber(contactId: String, isVerified: Boolean): Result<Unit> {
        return contactRepository.verifyContact(contactId, isVerified)
    }
}
