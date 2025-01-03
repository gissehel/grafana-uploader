package io.github.gissehel.grafana.grafanauploader.utils

import okhttp3.Request

fun Request.Builder.asJson() : Request.Builder =
    this.addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")

fun String.getUidFromVuid(): String {
    if (this.length > 40) {
        return getSha256(this).slice(0..40-1)
    }
    return this
}