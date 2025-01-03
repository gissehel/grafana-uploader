package io.github.gissehel.grafana.grafanauploader.model.credential

import io.github.gissehel.grafana.grafanauploader.model.Credential
import okhttp3.Request

class CredentialWithToken(val token: String): Credential() {
    override fun updateHeaders(requestBuilder: Request.Builder): Request.Builder = requestBuilder.addHeader("Authorization", "Bearer $token")
}