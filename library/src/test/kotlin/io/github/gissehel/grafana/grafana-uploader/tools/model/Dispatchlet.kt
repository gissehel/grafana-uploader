package io.github.gissehel.grafana.grafanauploader.tools.model

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class Dispatchlet (
    val name: String
) {
    var _testRequest: ((RecordedRequest) -> Boolean)? = null
    var _responseGetter: ((RecordedRequest) -> MockResponse?)? = null

    fun test(request: RecordedRequest) : Boolean {
        return _testRequest?.invoke(request) ?: true
    }

    fun getResponse(request: RecordedRequest) : MockResponse? {
        return _responseGetter?.invoke(request)
    }

    fun testRequest(block: ((RecordedRequest) -> Boolean)?) : Dispatchlet {
        _testRequest = block
        return this
    }
    fun responseGetter(block: ((RecordedRequest) -> MockResponse?)?) : Dispatchlet {
        _responseGetter = block
        return this
    }


}