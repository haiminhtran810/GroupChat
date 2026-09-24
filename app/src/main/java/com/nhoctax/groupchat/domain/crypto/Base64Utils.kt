package com.nhoctax.groupchat.domain.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Base64Utils {

    fun encode(bytes: ByteArray): String {
        return Base64.encode(bytes)
    }

    fun decode(base64String: String): ByteArray {
        return Base64.decode(base64String)
    }
}
