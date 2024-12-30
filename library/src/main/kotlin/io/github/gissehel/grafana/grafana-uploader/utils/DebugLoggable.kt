package io.github.gissehel.grafana.uploader.utils

open class DebugLoggable(
    private val onDebug : ((String) -> Unit)? = null,
) {
    fun debugLog(message : String) : Unit = onDebug?.let { onDebug -> onDebug(message) } ?: Unit
}