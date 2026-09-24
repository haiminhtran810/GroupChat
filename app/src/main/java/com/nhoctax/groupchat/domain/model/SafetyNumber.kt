package com.nhoctax.groupchat.domain.model

data class SafetyNumber(
    val user1Id: String,
    val user2Id: String,
    val displayFingerprint: String,
    val rawHash: ByteArray,
    val isVerified: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SafetyNumber

        if (user1Id != other.user1Id) return false
        if (user2Id != other.user2Id) return false
        if (displayFingerprint != other.displayFingerprint) return false
        if (!rawHash.contentEquals(other.rawHash)) return false
        if (isVerified != other.isVerified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user1Id.hashCode()
        result = 31 * result + user2Id.hashCode()
        result = 31 * result + displayFingerprint.hashCode()
        result = 31 * result + rawHash.contentHashCode()
        result = 31 * result + isVerified.hashCode()
        return result
    }
}
