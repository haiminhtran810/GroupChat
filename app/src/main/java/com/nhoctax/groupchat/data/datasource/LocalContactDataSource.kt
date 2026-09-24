package com.nhoctax.groupchat.data.datasource

import com.nhoctax.groupchat.domain.model.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class LocalContactDataSource(
    initialContacts: List<Contact> = emptyList()
) {
    private val contactsMap = ConcurrentHashMap<String, Contact>()
    private val _contactsFlow = MutableStateFlow<List<Contact>>(emptyList())

    init {
        initialContacts.forEach { contactsMap[it.id] = it }
        updateFlow()
    }

    val contactsFlow: Flow<List<Contact>> = _contactsFlow

    fun getContactFlow(contactId: String): Flow<Contact?> {
        return _contactsFlow.map { contacts ->
            contacts.find { it.id == contactId }
        }
    }

    fun getContact(contactId: String): Contact? {
        return contactsMap[contactId]
    }

    fun saveContact(contact: Contact) {
        val existing = contactsMap[contact.id]
        val publicKeyChanged = existing?.publicKeyPem != null &&
                existing.publicKeyPem != contact.publicKeyPem
        contactsMap[contact.id] = when {
            existing == null || publicKeyChanged -> contact.copy(
                isVerified = false,
                verifiedPublicKeyPem = null
            )
            existing.isVerified -> contact.copy(
                isVerified = true,
                verifiedPublicKeyPem = existing.verifiedPublicKeyPem
            )
            else -> contact.copy(isVerified = false, verifiedPublicKeyPem = null)
        }
        updateFlow()
    }

    fun updateVerificationStatus(contactId: String, isVerified: Boolean) {
        val existing = contactsMap[contactId] ?: return
        contactsMap[contactId] = existing.copy(
            isVerified = isVerified,
            verifiedPublicKeyPem = if (isVerified) existing.publicKeyPem else null
        )
        updateFlow()
    }

    fun deleteContact(contactId: String) {
        contactsMap.remove(contactId)
        updateFlow()
    }

    fun clearAll() {
        contactsMap.clear()
        updateFlow()
    }

    private fun updateFlow() {
        _contactsFlow.value = contactsMap.values.sortedBy { it.name }
    }
}
