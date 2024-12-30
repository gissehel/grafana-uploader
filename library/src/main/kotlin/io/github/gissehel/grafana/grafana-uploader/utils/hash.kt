package io.github.gissehel.grafana.uploader.utils

import java.security.MessageDigest

fun getSha256(data: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}