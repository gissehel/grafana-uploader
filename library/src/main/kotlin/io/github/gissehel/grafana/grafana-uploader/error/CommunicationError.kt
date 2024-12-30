package io.github.gissehel.grafana.uploader.error

class CommunicationError(code: Int) : Exception() {
    val asCode: Int = code
}