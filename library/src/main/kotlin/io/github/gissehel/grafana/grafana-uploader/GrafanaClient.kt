package io.github.gissehel.grafana.uploader

import io.github.gissehel.grafana.uploader.error.CommunicationError
import io.github.gissehel.grafana.uploader.model.Folder
import io.github.gissehel.grafana.uploader.utils.DebugLoggable
import io.github.gissehel.grafana.uploader.utils.getUidFromVuid

class GrafanaClient(
    val token: String,
    val rootUrl: String,
    val onDebug : ((String) -> Unit)? = null
) : DebugLoggable(onDebug) {
    private var _http_client : HttpClient? = null
    private val httpClient : HttpClient get() {
        if (_http_client == null) {
            HttpClient(onDebug).also { _http_client = it }
        }
        return _http_client!!
    }

    private val getFoldersUrl: String get() = "${rootUrl}/api/folders"
    private fun getFolderUrl(uid: String): String = "${rootUrl}/api/folders/${uid}"

    fun createFolderIfNotExists(vuid: String, title: String, parentVuid: String) : Unit {
        var exists: Boolean = false
        val uid = vuid.getUidFromVuid()
        val parentUid = parentVuid.getUidFromVuid()
        try {
            httpClient.getJson(getFolderUrl(uid), token) { jsonElement ->
                debugLog("Json: $jsonElement")
                exists = true
            }
        } catch (e: CommunicationError) {
            debugLog("Communication error: [${e.asCode}]")
        }
        if (!exists) {
            val folder = Folder(uid = uid, title = title, parentUid = parentUid)
            httpClient.postJson(getFoldersUrl, token, folder) { jsonElement ->
                debugLog("Created folder [${uid}] ($vuid)")
                debugLog("Json: ${jsonElement}")
            }
        }
    }
}