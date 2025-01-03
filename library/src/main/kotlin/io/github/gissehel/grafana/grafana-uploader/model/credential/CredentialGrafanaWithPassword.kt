package io.github.gissehel.grafana.grafanauploader.model.credential

import io.github.gissehel.grafana.grafanauploader.model.Credential
import io.github.gissehel.grafana.grafanauploader.HttpClient
import io.github.gissehel.grafana.grafanauploader.error.NoSessionError
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Request
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

@IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible
class CredentialGrafanaWithPassword(
    val username: String,
    val password: String
): Credential() {
    var session: String? = null
    override fun ensureCredientialUsable(client: HttpClient) {
        val loginEntity = buildJsonObject {
            put("user", JsonPrimitive(username))
            put("password", JsonPrimitive(password))
        }
        client.postJson("/login",  loginEntity, usingNoAuthentification()) { jsonElement, response ->
            response.headers.iterator().asSequence().toList()
                .filter { (name, value) -> name.lowercase() == "Set-Cookie".lowercase() }
                .mapNotNull { (name, value) -> Cookie.parse(client.rootUrl.toHttpUrl(), value) }
                .firstOrNull { cookie -> cookie.name.lowercase() == "grafana_session".lowercase() }
                ?.let { cookie -> session=cookie.value }
        }
        if (session == null) {
            throw NoSessionError()
        }
    }
    override fun updateHeaders(requestBuilder: Request.Builder): Request.Builder {
        if (session != null) {
            return requestBuilder.header("Cookie", "grafana_session=${session}")
        }
        return requestBuilder
    }
}