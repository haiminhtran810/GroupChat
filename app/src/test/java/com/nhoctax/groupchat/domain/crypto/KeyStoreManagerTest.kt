package com.nhoctax.groupchat.domain.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyStoreManagerTest {

    private lateinit var keyStoreManager: KeyStoreManager

    @Before
    fun setUp() {
        keyStoreManager = KeyStoreManager()
    }

    @Test
    fun testGenerateAndRetrieveKeyPair() {
        val alias = "test_identity_key"
        val keyPair = keyStoreManager.generateKeyPair(alias)

        assertNotNull(keyPair)
        assertNotNull(keyPair.public)
        assertNotNull(keyPair.private)

        val retrievedKeyPair = keyStoreManager.getKeyPair(alias)
        assertNotNull(retrievedKeyPair)
        assertEquals(keyPair.public, retrievedKeyPair?.public)
    }

    @Test
    fun testExportAndParsePublicKeyPem() {
        val alias = "pem_test_key"
        val keyPair = keyStoreManager.getOrCreateKeyPair(alias)

        val pem = keyStoreManager.exportPublicKeyPem(keyPair.public)
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"))
        assertTrue(pem.endsWith("-----END PUBLIC KEY-----"))

        val parsedKey = keyStoreManager.parsePublicKeyPem(pem)
        assertNotNull(parsedKey)
        assertEquals(keyPair.public, parsedKey)
    }

    @Test
    fun testDeleteKeyPair() {
        val alias = "delete_test_key"
        keyStoreManager.generateKeyPair(alias)

        assertNotNull(keyStoreManager.getKeyPair(alias))

        keyStoreManager.deleteKeyPair(alias)
        assertNull(keyStoreManager.getKeyPair(alias))
    }
}
