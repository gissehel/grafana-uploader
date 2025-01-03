package io.github.gissehel.grafana.grafanauploader.error

class CommunicationError(code: Int) : Exception() {
    val asCode: Int = code
}