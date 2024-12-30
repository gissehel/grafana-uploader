package io.github.gissehel.grafana.uploader

// import io.github.infeez.kotlinmockserver.dsl.http.okhttp.okHttpMockServer
// import io.github.infeez.kotlinmockserver.extensions.change
// import io.github.infeez.kotlinmockserver.server.ServerConfiguration
// import mock
import io.github.gissehel.grafana.uploader.tools.TestDispatcher
import io.github.gissehel.grafana.uploader.tools.model.Dispatchlet
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test


class HttpClientTest {
    val testDispatcher = TestDispatcher()
    val mockServer = MockWebServer().apply {
        dispatcher = testDispatcher
        start()
    }

    fun getRootUrl() : String = mockServer.url("").toString() // url like "http://localhost:9999/"

    @BeforeEach
    fun beforeEach() {
        testDispatcher.clear()
    }

    @AfterEach
    fun afterEach() {
    }

    @Test
    fun `basic call should work as expected`() {
        val httpClient = HttpClient()
        testDispatcher.add(dispatchletFoldersWhenEmpty)

        httpClient.getJson("${getRootUrl()}api/folders","grut") { jsonElement ->
            val jsonArray = jsonElement.jsonArray
            assert(! jsonArray.isEmpty()) {
                "Array should not be empty"
            }
            assert(jsonArray.count() == 2) {
                "Array should have 2 elements"
            }
            val testFolderList  = jsonArray.filter { jsonElement ->
                val obj = jsonElement.jsonObject
                obj.containsKey("uid") && obj.get("uid")?.jsonPrimitive?.content == "test-folder"
            }
            assert(testFolderList.count() == 1) {
                "There should be exactly one test folder"
            }
        }
    }

    @Test
    fun `API call should provide the right token`() {
        val httpClient = HttpClient()
        testDispatcher.add(dispatchletFoldersWhenEmpty)

        httpClient.getJson("${getRootUrl()}api/folders","grut") { jsonElement ->
            val request = mockServer.takeRequest()
            val auth = request.headers.get("Authorization")
            assert(auth != null) {
                "There should be an authorization in the request's headers"
            }
            assert(auth == "Bearer grut") {
                "The token should be passed to the request's header authorization field"
            }
        }
    }

    private val dispatchletFoldersWhenEmpty = Dispatchlet("folders")
        .testRequest { recordedRequest ->
            recordedRequest.path == "/api/folders" && recordedRequest.method == "GET"
        }
        .responseGetter {
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":32,"uid":"test-folder","title":"Test folder"},{"id":33,"uid":dummy,"title":"Dummy folder"}]""")
        }

}