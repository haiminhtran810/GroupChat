package com.nhoctax.groupchat.data.repository

import com.nhoctax.groupchat.data.datasource.LocalContactDataSource
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

class ContactRepositoryImpl(
    private val localContactDataSource: LocalContactDataSource
) : ContactRepository {

    override fun getContacts(): Flow<List<Contact>> {
        return localContactDataSource.contactsFlow
    }

    override fun getContactById(contactId: String): Flow<Contact?> {
        return localContactDataSource.getContactFlow(contactId)
    }

    override suspend fun getContact(contactId: String): Contact? {
        return localContactDataSource.getContact(contactId)
    }

    override suspend fun addContact(contact: Contact): Result<Unit> {
        return try {
            localContactDataSource.saveContact(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: Contact): Result<Unit> {
        return addContact(contact)
    }

    override suspend fun verifyContact(contactId: String, isVerified: Boolean): Result<Unit> {
        return try {
            localContactDataSource.updateVerificationStatus(contactId, isVerified)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
