package com.nhoctax.groupchat.domain.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap

class KeyStoreManager(
    val keyStoreType: String = ANDROID_KEYSTORE,
) {
    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_SIZE = 2048
        const val KEY_ALGORITHM_RSA = "RSA"

        private const val PEM_PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----"
        private const val PEM_PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----"
    }

    private val memoryKeyStore = ConcurrentHashMap<String, KeyPair>()

    private val isAndroidRuntime: Boolean
        get() = System.getProperty("java.vm.name")?.contains("Dalvik", ignoreCase = true) == true

    private val isAndroidKeyStoreAvailable: Boolean
        get() = try {
            Security.getProviders().any { it.name == ANDROID_KEYSTORE } &&
                    try {
                        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                        true
                    } catch (_: Exception) {
                        false
                    }
        } catch (_: Exception) {
            false
        }

    fun getOrCreateKeyPair(alias: String): KeyPair {
        return getKeyPair(alias) ?: generateKeyPair(alias)
    }

    fun generateKeyPair(alias: String): KeyPair {
        if (isAndroidKeyStoreAvailable) {
            return try {
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
                            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setKeySize(DEFAULT_KEY_SIZE)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .build()

                keyPairGenerator.initialize(keyGenParameterSpec)
                keyPairGenerator.generateKeyPair()
            } catch (error: Exception) {
                if (isAndroidRuntime) {
                    throw IllegalStateException("Android KeyStore key generation failed", error)
                }
                generateJvmKeyPair(alias)
            }
        } else {
            if (isAndroidRuntime) {
                throw IllegalStateException("Android KeyStore is unavailable")
            }
            return generateJvmKeyPair(alias)
        }
    }

    private fun generateJvmKeyPair(alias: String): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA)
        keyPairGenerator.initialize(DEFAULT_KEY_SIZE)
        val keyPair = keyPairGenerator.generateKeyPair()
        memoryKeyStore[alias] = keyPair
        return keyPair
    }

    fun getKeyPair(alias: String): KeyPair? {
        val publicKey = getPublicKey(alias)
        val privateKey = getPrivateKey(alias)
        if (publicKey != null && privateKey != null) {
            return KeyPair(publicKey, privateKey)
        }
        return memoryKeyStore[alias]
    }

    fun getPublicKey(alias: String): PublicKey? {
        if (isAndroidKeyStoreAvailable) {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(alias)) {
                    val certificate = keyStore.getCertificate(alias)
                    if (certificate != null) {
                        return certificate.publicKey
                    }
                }
            } catch (_: Exception) {
            }
        }
        return memoryKeyStore[alias]?.public
    }

    fun getPrivateKey(alias: String): PrivateKey? {
        if (isAndroidKeyStoreAvailable) {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(alias)) {
                    val entry = keyStore.getEntry(alias, null)
                    if (entry is KeyStore.PrivateKeyEntry) {
                        return entry.privateKey
                    }
                }
            } catch (_: Exception) {
            }
        }
        return memoryKeyStore[alias]?.private
    }

    fun deleteKeyPair(alias: String) {
        memoryKeyStore.remove(alias)
        if (isAndroidKeyStoreAvailable) {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(alias)) {
                    keyStore.deleteEntry(alias)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun exportPublicKeyPem(publicKey: PublicKey): String {
        val encoded = Base64Utils.encode(publicKey.encoded)
        val chunked = encoded.chunked(64).joinToString("\n")
        return "$PEM_PUBLIC_KEY_HEADER\n$chunked\n$PEM_PUBLIC_KEY_FOOTER"
    }

    fun parsePublicKeyPem(pemString: String): PublicKey {
        val cleanPem = pemString
            .replace(PEM_PUBLIC_KEY_HEADER, "")
            .replace(PEM_PUBLIC_KEY_FOOTER, "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64Utils.decode(cleanPem)
        val keySpec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM_RSA)
        return keyFactory.generatePublic(keySpec)
    }
}
