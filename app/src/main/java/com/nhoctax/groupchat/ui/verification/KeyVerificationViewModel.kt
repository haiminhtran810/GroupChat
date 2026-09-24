package com.nhoctax.groupchat.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.SafetyNumber
import com.nhoctax.groupchat.domain.repository.ContactRepository
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KeyVerificationUiState(
    val contactId: String? = null,
    val contact: Contact? = null,
    val safetyNumber: SafetyNumber? = null,
    val userPublicKeyPem: String? = null,
    val contactPublicKeyPem: String? = null,
    val isVerified: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

class KeyVerificationViewModel(
    private val securityRepository: SecurityRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeyVerificationUiState())
    val uiState: StateFlow<KeyVerificationUiState> = _uiState.asStateFlow()

    private var safetyNumberJob: Job? = null
    private var contactJob: Job? = null

    fun loadContactId(contactId: String?) {
        if (contactId == null) {
            _uiState.value = KeyVerificationUiState()
            return
        }

        if (_uiState.value.contactId == contactId) {
            return
        }

        _uiState.value = KeyVerificationUiState(
            contactId = contactId,
            isLoading = true
        )

        safetyNumberJob?.cancel()
        contactJob?.cancel()

        viewModelScope.launch {
            val userKeyPem = securityRepository.getUserPublicKeyPem()
            _uiState.update { it.copy(userPublicKeyPem = userKeyPem) }
        }

        contactJob = viewModelScope.launch {
            contactRepository.getContactById(contactId).collect { contact ->
                _uiState.update { state ->
                    state.copy(
                        contact = contact,
                        contactPublicKeyPem = contact?.publicKeyPem,
                        isVerified = contact?.isVerified == true
                    )
                }
            }
        }

        safetyNumberJob = viewModelScope.launch {
            securityRepository.getSafetyNumber(contactId).collect { safetyNumber ->
                _uiState.update { state ->
                    state.copy(
                        safetyNumber = safetyNumber,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleVerification() {
        val currentState = _uiState.value
        val contactId = currentState.contactId ?: return
        val newVerifiedState = !currentState.isVerified

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = securityRepository.verifySafetyNumber(contactId, newVerifiedState)
            result.onSuccess {
                _uiState.update { state ->
                    if (state.contactId != contactId) return@update state
                    state.copy(
                        isVerified = newVerifiedState,
                        isLoading = false,
                        statusMessage = if (newVerifiedState) {
                            "Safety number verified successfully!"
                        } else {
                            "Verification un-marked."
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    if (state.contactId != contactId) return@update state
                    state.copy(
                        isLoading = false,
                        statusMessage = "Error: ${error.message}"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(
        private val securityRepository: SecurityRepository,
        private val contactRepository: ContactRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KeyVerificationViewModel(securityRepository, contactRepository) as T
        }
    }
}
