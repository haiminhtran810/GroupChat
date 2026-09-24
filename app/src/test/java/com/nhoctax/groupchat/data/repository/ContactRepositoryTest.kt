package com.nhoctax.groupchat.data.repository

import com.nhoctax.groupchat.data.datasource.LocalContactDataSource
import com.nhoctax.groupchat.domain.model.Contact
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactRepositoryTest {

    private lateinit var localContactDataSource: LocalContactDataSource
    private lateinit var contactRepository: ContactRepositoryImpl

    @Before
    fun setUp() {
        localContactDataSource = LocalContactDataSource()
        contactRepository = ContactRepositoryImpl(localContactDataSource)
    }

    @Test
    fun addContactAndGetContacts_returnsAddedContactsSortedByName() = runTest {
        val contact1 = Contact(id = "c1", name = "Charlie")
        val contact2 = Contact(id = "c2", name = "Alice")

        contactRepository.addContact(contact1)
        contactRepository.addContact(contact2)

        val contacts = contactRepository.getContacts().first()

        assertEquals(2, contacts.size)
        assertEquals("Alice", contacts[0].name)
        assertEquals("Charlie", contacts[1].name)
    }

    @Test
    fun getContactById_returnsMatchingContactFlow() = runTest {
        val contact = Contact(id = "c1", name = "Bob", phoneNumber = "123456")
        contactRepository.addContact(contact)

        val retrieved = contactRepository.getContactById("c1").first()

        assertNotNull(retrieved)
        assertEquals("Bob", retrieved?.name)
        assertEquals("123456", retrieved?.phoneNumber)
    }

    @Test
    fun verifyContact_updatesVerificationState() = runTest {
        val contact = Contact(id = "c1", name = "Alice", publicKeyPem = "key-one", isVerified = false)
        contactRepository.addContact(contact)

        contactRepository.verifyContact("c1", true)

        val updated = contactRepository.getContact("c1")
        assertTrue(updated?.isVerified == true)
        assertEquals("key-one", updated?.verifiedPublicKeyPem)

        contactRepository.verifyContact("c1", false)
        val reverted = contactRepository.getContact("c1")
        assertFalse(reverted?.isVerified == true)
    }

    @Test
    fun changingPublicKey_invalidatesPreviousVerification() = runTest {
        contactRepository.addContact(
            Contact(id = "c1", name = "Alice", publicKeyPem = "key-one")
        )
        contactRepository.verifyContact("c1", true)

        contactRepository.updateContact(
            Contact(
                id = "c1",
                name = "Alice",
                publicKeyPem = "key-two",
                isVerified = true,
                verifiedPublicKeyPem = "key-one"
            )
        )

        val updated = contactRepository.getContact("c1")
        assertFalse(updated?.isVerified == true)
        assertEquals(null, updated?.verifiedPublicKeyPem)
    }
}
