package io.github.gissehel.grafana.uploader.utils

import okhttp3.Request

fun Request.Builder.asJsonWithToken(token: String) : Request.Builder {
    this.addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "Bearer ${token}")

    return this
}

fun String.getUidFromVuid(): String {
    if (this.length > 40) {
        return getSha256(this).slice(0..40-1)
    }
    return this
}