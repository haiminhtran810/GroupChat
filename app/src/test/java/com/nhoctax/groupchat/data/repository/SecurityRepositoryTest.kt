package com.nhoctax.groupchat.data.repository

import com.nhoctax.groupchat.data.datasource.LocalContactDataSource
import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.crypto.SafetyNumberGenerator
import com.nhoctax.groupchat.domain.model.Contact
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityRepositoryTest {

    private lateinit var keyStoreManager: KeyStoreManager
    private lateinit var safetyNumberGenerator: SafetyNumberGenerator
    private lateinit var localContactDataSource: LocalContactDataSource
    private lateinit var contactRepository: ContactRepositoryImpl
    private lateinit var securityRepository: SecurityRepositoryImpl

    @Before
    fun setUp() {
        keyStoreManager = KeyStoreManager()
        safetyNumberGenerator = SafetyNumberGenerator()
        localContactDataSource = LocalContactDataSource()
        contactRepository = ContactRepositoryImpl(localContactDataSource)
        securityRepository = SecurityRepositoryImpl(
            keyStoreManager = keyStoreManager,
            safetyNumberGenerator = safetyNumberGenerator,
            contactRepository = contactRepository
        )
    }

    @Test
    fun getOrCreateUserKeyPair_generatesValidKeyPair() = runTest {
        val keyPair = securityRepository.getOrCreateUserKeyPair("test_alias")
        assertNotNull(keyPair.public)
        assertNotNull(keyPair.private)

        val pem = securityRepository.getUserPublicKeyPem("test_alias")
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"))
    }

    @Test
    fun generateSafetyNumber_computesConsistentSafetyNumberWithContact() = runTest {
        // Create user key
        securityRepository.getOrCreateUserKeyPair()

        // Create contact key
        val contactKeyPair = keyStoreManager.generateKeyPair("contact_bob")
        val contactPem = keyStoreManager.exportPublicKeyPem(contactKeyPair.public)

        val contact = Contact(
            id = "bob",
            name = "Bob",
            publicKeyPem = contactPem,
            isVerified = false
        )
        contactRepository.addContact(contact)

        val result = securityRepository.generateSafetyNumber("bob")
        assertTrue(result.isSuccess)

        val safetyNumber = result.getOrThrow()
        assertNotNull(safetyNumber.displayFingerprint)
        assertEquals(6, safetyNumber.displayFingerprint.split(" ").size)
    }

    @Test
    fun verifySafetyNumber_updatesContactVerificationAndSafetyNumberFlow() = runTest {
        securityRepository.getOrCreateUserKeyPair()
        val contactKeyPair = keyStoreManager.generateKeyPair("contact_alice")
        val contactPem = keyStoreManager.exportPublicKeyPem(contactKeyPair.public)

        val contact = Contact(
            id = "alice",
            name = "Alice",
            publicKeyPem = contactPem,
            isVerified = false
        )
        contactRepository.addContact(contact)

        val initialSn = securityRepository.getSafetyNumber("alice").first()
        assertEquals(false, initialSn?.isVerified)

        securityRepository.verifySafetyNumber("alice", true)

        val updatedSn = securityRepository.getSafetyNumber("alice").first()
        assertEquals(true, updatedSn?.isVerified)
    }
}
