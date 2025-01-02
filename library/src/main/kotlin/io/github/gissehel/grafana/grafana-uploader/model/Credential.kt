package io.github.gissehel.grafana.`grafana-uploader`.model

import io.github.gissehel.grafana.`grafana-uploader`.HttpClient
import io.github.gissehel.grafana.`grafana-uploader`.model.credential.CredentialGrafanaWithPassword
import io.github.gissehel.grafana.`grafana-uploader`.model.credential.CredentialNone
import io.github.gissehel.grafana.`grafana-uploader`.model.credential.CredentialWithToken
import io.github.gissehel.grafana.`grafana-uploader`.model.credential.IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible
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

