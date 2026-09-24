package com.nhoctax.groupchat.domain.repository

import com.nhoctax.groupchat.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContacts(): Flow<List<Contact>>
    fun getContactById(contactId: String): Flow<Contact?>
    suspend fun getContact(contactId: String): Contact?
    suspend fun addContact(contact: Contact): Result<Unit>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun verifyContact(contactId: String, isVerified: Boolean): Result<Unit>
}
