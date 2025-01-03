package io.github.gissehel.grafana.grafanauploader.model

import io.github.gissehel.grafana.grafanauploader.HttpClient
import io.github.gissehel.grafana.grafanauploader.model.credential.CredentialGrafanaWithPassword
import io.github.gissehel.grafana.grafanauploader.model.credential.CredentialNone
import io.github.gissehel.grafana.grafanauploader.model.credential.CredentialWithToken
import io.github.gissehel.grafana.grafanauploader.model.credential.IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible
import okhttp3.Request

abstract class Credential {
    open fun ensureCredientialUsable(client: HttpClient) {}
    open fun updateHeaders(requestBuilder: Request.Builder): Request.Builder = requestBuilder
    companion object {
        fun usingToken(token: String) = CredentialWithToken(token)
        fun usingNoAuthentification() = CredentialNone()
        @IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible
        fun usingGrafanaUsernamePassword(username: String, password: String) = CredentialGrafanaWithPassword(username, password)
    }
}

fun Request.Builder.useCredential(credential: Credential) = credential.updateHeaders(this)

