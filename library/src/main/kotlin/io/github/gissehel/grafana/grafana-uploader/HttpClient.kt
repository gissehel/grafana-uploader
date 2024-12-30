package io.github.gissehel.grafana.uploader
import io.github.gissehel.grafana.uploader.error.CommunicationError
import io.github.gissehel.grafana.uploader.error.NoResponseError
import io.github.gissehel.grafana.uploader.utils.DebugLoggable
import io.github.gissehel.grafana.uploader.utils.asJsonWithToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class HttpClient (
    onDebug : ((String) -> Unit)? = null,
) : DebugLoggable(onDebug = onDebug) {
    private val client = OkHttpClient()

    fun handleCallResponse(request: Request, onSuccess: (JsonElement) -> Unit) {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw NoResponseError()
                debugLog("Got result [${body}]")
                onSuccess(Json.parseToJsonElement(body))
            } else {
                debugLog("Got CommunicationError with code [${response.code}]")
                throw CommunicationError(code=response.code)
            }
        }
    }

    fun getJson(url: String, token: String, onSuccess: (JsonElement) -> Unit) {
        debugLog("Calling getJson on [${url}]")
        val request = Request.Builder()
            .url(url)
            .get()
            .asJsonWithToken(token)
            .build()

        handleCallResponse(request, onSuccess)
    }

    inline fun <reified T> postJson(url: String, token: String, entity: T, noinline onSuccess: (JsonElement) -> Unit) {
        val mediaType = "application/json".toMediaType()
        val jsonEntity = Json.encodeToString(entity)

        debugLog("Calling postJson on [${url}] with object [${jsonEntity}]")
        val body = jsonEntity.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .asJsonWithToken(token)
            .build()

        handleCallResponse(request, onSuccess)
    }
}