package com.nhoctax.groupchat.domain.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafetyNumberGeneratorTest {

    private lateinit var safetyNumberGenerator: SafetyNumberGenerator
    private lateinit var keyStoreManager: KeyStoreManager

    @Before
    fun setUp() {
        safetyNumberGenerator = SafetyNumberGenerator()
        keyStoreManager = KeyStoreManager()
    }

    @Test
    fun testGenerateSafetyNumberFormat() {
        val aliceKeyPair = keyStoreManager.generateKeyPair("alice")
        val bobKeyPair = keyStoreManager.generateKeyPair("bob")

        val safetyNumber = safetyNumberGenerator.generateSafetyNumber(
            user1Id = "alice_id",
            publicKey1 = aliceKeyPair.public,
            user2Id = "bob_id",
            publicKey2 = bobKeyPair.public
        )

        assertNotNull(safetyNumber)
        val formatted = safetyNumber.displayFingerprint

        // Expected format: 6 groups of 5 digits separated by space (30 digits total + 5 spaces = 35 chars)
        val parts = formatted.split(" ")
        assertEquals(6, parts.size)
        parts.forEach { part ->
            assertEquals(5, part.length)
            assertTrue(part.all { it.isDigit() })
        }
    }

    @Test
    fun testSymmetryOfSafetyNumber() {
        val aliceKeyPair = keyStoreManager.generateKeyPair("alice")
        val bobKeyPair = keyStoreManager.generateKeyPair("bob")

        val safetyNumber1 = safetyNumberGenerator.generateSafetyNumber(
            user1Id = "user_alice",
            publicKey1 = aliceKeyPair.public,
            user2Id = "user_bob",
            publicKey2 = bobKeyPair.public
        )

        val safetyNumber2 = safetyNumberGenerator.generateSafetyNumber(
            user1Id = "user_bob",
            publicKey1 = bobKeyPair.public,
            user2Id = "user_alice",
            publicKey2 = aliceKeyPair.public
        )

        assertEquals(safetyNumber1.displayFingerprint, safetyNumber2.displayFingerprint)
        assertTrue(safetyNumber1.rawHash.contentEquals(safetyNumber2.rawHash))
    }
}
