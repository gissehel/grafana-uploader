package io.github.gissehel.grafana.uploader.tools

import io.github.gissehel.grafana.uploader.tools.model.Dispatchlet
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class TestDispatcher : Dispatcher() {
    private var dispatchlets : MutableList<Dispatchlet> = mutableListOf()
    private var dispatchletsByName : MutableMap<String, Dispatchlet> = mutableMapOf()
    val namesDispatched : MutableList<String> = mutableListOf()

    fun add(dispatchlet: Dispatchlet) {
        val index = dispatchlets.indexOfFirst { dispatchlet.name == it.name }
        if (index > -1) {
            dispatchlets[index] = dispatchlet
        } else {
            dispatchlets.add(dispatchlet)
        }
        dispatchletsByName[dispatchlet.name] = dispatchlet
    }

    fun remove(name: String) {
        val index = dispatchlets.indexOfFirst { name == it.name }
        if (index > -1) {
            dispatchlets.drop(index)
        }
        if (dispatchletsByName.contains(name)) {
            dispatchletsByName.remove(name)
        }
    }

    fun clear() {
        dispatchlets.clear()
        dispatchletsByName.clear()
        namesDispatched.clear()
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        for(dispatchlet in dispatchlets) {
            if (dispatchlet.test(request)) {
                val response = dispatchlet.getResponse(request)
                if (response != null) {
                    namesDispatched.add(dispatchlet.name)
                    return response
                }
            }
        }
        return MockResponse().setResponseCode(404)
    }
}