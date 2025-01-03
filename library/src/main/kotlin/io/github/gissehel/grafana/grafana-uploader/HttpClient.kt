package io.github.gissehel.grafana.grafanauploader
import io.github.gissehel.grafana.grafanauploader.error.CommunicationError
import io.github.gissehel.grafana.grafanauploader.error.NoResponseError
import io.github.gissehel.grafana.grafanauploader.model.Credential
import io.github.gissehel.grafana.grafanauploader.model.useCredential
import io.github.gissehel.grafana.grafanauploader.utils.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class HttpClient (
    val rootUrl: String,
    val credential: Credential,
    onDebug : ((String) -> Unit)? = null,
) : DebugLoggable(onDebug = onDebug) {
    private val client = OkHttpClient()

    fun handleCallResponse(request: Request, onSuccess: (JsonElement, response: Response) -> Unit) {
        client.newCall(request).execute().use { response ->
            debugLog("responseHeader: ${response.headers.asSequence().toList().map{ header -> "[${header.first}=${header.second}]"}.joinToString("/")}")
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw NoResponseError()
                debugLog("Got result [${body}]")
                onSuccess(Json.parseToJsonElement(body), response)
            } else {
                debugLog("Got CommunicationError with code [${response.code}]")
                throw CommunicationError(code=response.code)
            }
        }
    }

    fun getJson(url: String, onSuccess: (JsonElement) -> Unit) {
        return getJson(url) { jsonElement, response ->
            onSuccess(jsonElement)
        }
    }

    fun getJson(url: String, onSuccess: (JsonElement, Response) -> Unit) {
        debugLog("Calling getJson on [${url}]")
        ensureCredentials(credential)
        val request = Request.Builder()
            .url(rootUrl + url)
            .get()
            .asJson()
            .useCredential(credential)
            .build()

        handleCallResponse(request, onSuccess)
    }

    inline fun <reified T> postJson(url: String, entity: T, noinline onSuccess: (JsonElement) -> Unit) {
        return postJson(url, entity) { jsonElement, response ->
            onSuccess(jsonElement)
        }
    }

    inline fun <reified T> postJson(url: String, entity: T, noinline onSuccess: (JsonElement, Response) -> Unit) {
        return postJson(url, entity, credential, onSuccess)
    }

    inline fun <reified T> postJson(url: String, entity: T, credential: Credential, noinline onSuccess: (JsonElement, Response) -> Unit) {
        val jsonEntity = Json.encodeToString(entity)
        return postJsonString(url, jsonEntity, credential, onSuccess)
    }

    fun postJsonString(url: String, entityAsJsonString: String, credential: Credential, onSuccess: (JsonElement, Response) -> Unit) {
        val mediaType = "application/json".toMediaType()

        debugLog("Calling postJson on [${url}] with object [${entityAsJsonString}]")
        ensureCredentials(credential)
        val body = entityAsJsonString.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(rootUrl + url)
            .post(body)
            .asJson()
            .useCredential(credential)
            .build()

        handleCallResponse(request, onSuccess)
    }

    fun ensureCredentials(credential: Credential) = credential.ensureCredientialUsable(this)


}