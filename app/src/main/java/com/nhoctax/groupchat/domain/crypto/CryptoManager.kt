package com.nhoctax.groupchat.domain.crypto

import com.nhoctax.groupchat.domain.model.EncryptedPayload
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    companion object {
        const val AES_KEY_SIZE = 256
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128

        const val CIPHER_TRANSFORMATION_AES = "AES/GCM/NoPadding"
        const val CIPHER_TRANSFORMATION_RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

        private val OAEP_SPEC = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            // AndroidKeyStore's OAEP SHA-256 implementation uses SHA-1 for MGF1.
            // Supplying it explicitly keeps encryption/decryption interoperable
            // between AndroidKeyStore and software public-key providers.
            MGF1ParameterSpec.SHA1,
            PSource.PSpecified.DEFAULT
        )
    }

    private val secureRandom = SecureRandom()

    fun generateSymmetricKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(AES_KEY_SIZE, secureRandom)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String, recipientPublicKey: PublicKey): EncryptedPayload {
        return encryptBytes(plainText.toByteArray(Charsets.UTF_8), recipientPublicKey)
    }

    fun encryptBytes(data: ByteArray, recipientPublicKey: PublicKey): EncryptedPayload {
        val aesKey = generateSymmetricKey()

        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val aesCipher = Cipher.getInstance(CIPHER_TRANSFORMATION_AES)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        val ciphertext = aesCipher.doFinal(data)

        val rsaCipher = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA)
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey, OAEP_SPEC)
        val encryptedSymmetricKey = rsaCipher.doFinal(aesKey.encoded)

        return EncryptedPayload(
            ciphertext = ciphertext,
            iv = iv,
            encryptedSymmetricKey = encryptedSymmetricKey,
            algorithm = EncryptedPayload.DEFAULT_ALGORITHM
        )
    }

    fun decrypt(payload: EncryptedPayload, recipientPrivateKey: PrivateKey): String {
        val decryptedBytes = decryptBytes(payload, recipientPrivateKey)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun decryptBytes(payload: EncryptedPayload, recipientPrivateKey: PrivateKey): ByteArray {
        val rsaCipher = Cipher.getInstance(CIPHER_TRANSFORMATION_RSA)
        rsaCipher.init(Cipher.DECRYPT_MODE, recipientPrivateKey, OAEP_SPEC)
        val aesKeyBytes = rsaCipher.doFinal(payload.encryptedSymmetricKey)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        val aesCipher = Cipher.getInstance(CIPHER_TRANSFORMATION_AES)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, payload.iv)
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)

        return aesCipher.doFinal(payload.ciphertext)
    }
}
