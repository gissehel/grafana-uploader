package io.github.gissehel.grafana.grafanauploader

import io.github.gissehel.grafana.grafanauploader.error.CommunicationError
import io.github.gissehel.grafana.grafanauploader.model.Credential
import io.github.gissehel.grafana.grafanauploader.model.credential.IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible
import io.github.gissehel.grafana.grafanauploader.tools.TestDispatcher
import io.github.gissehel.grafana.grafanauploader.tools.model.Dispatchlet
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

@OptIn(IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible::class)

class HttpClientTest {
    val testDispatcher = TestDispatcher()
    val mockServer = MockWebServer().apply {
        dispatcher = testDispatcher
        start()
    }

    fun getRootUrl() : String = mockServer.url("").toString().dropLast(1) // url like "http://localhost:9999"
    fun getHttpClient() = HttpClient(getRootUrl(), Credential.usingToken("grut"))
    fun getHttpClient(username: String, password: String) = HttpClient(getRootUrl(), Credential.usingGrafanaUsernamePassword(username, password)) {
        println(it)
    }

    @BeforeEach
    fun beforeEach() {
        testDispatcher.clear()
    }

    @AfterEach
    fun afterEach() {
    }

    @Test
    fun `basic call should work as expected`() {
        val httpClient = getHttpClient()
        testDispatcher.add(dispatchletFoldersWhenEmpty)

        httpClient.getJson("/api/folders") { jsonElement ->
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
        val httpClient = getHttpClient()
        testDispatcher.add(dispatchletFoldersWhenEmpty)

        httpClient.getJson("/api/folders") { jsonElement ->
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

    @Test
    fun `getJson should fail when called on an endpoint that is not grafana compatible`() {
        val httpClient = getHttpClient()

        assertFailsWith<CommunicationError> {
            httpClient.getJson("/api/folders") { jsonElement ->
                assert(false) {
                    "Should not arrive here"
                }
            }
        }
    }

    @Test
    fun `API call should be callable with username and password`() {
        val httpClient = getHttpClient("barnabo", "Barnabo67")
        testDispatcher.add(dispatchletLogin)
        testDispatcher.add(dispatchletFoldersWhenEmpty)

        httpClient.getJson("/api/folders") { jsonElement ->
            assert(mockServer.requestCount == 2) {
                "It should have created 2 requests (not ${mockServer.requestCount})"
            }
            mockServer.takeRequest().let { request ->
                val auth = request.headers.get("Authorization")
                assert(auth == null) {
                    "There should be no authorization in the first request's headers"
                }
                val cookie = request.headers.get("Cookie")
                assert(cookie == null) {
                    "There should be no cookie in the request's headers"
                }
            }
            mockServer.takeRequest().let { request ->
                val auth = request.headers.get("Authorization")
                assert(auth == null) {
                    "There should be no authorization in the second request's headers"
                }
                val cookie = request.headers.get("Cookie")
                assert(cookie == "grafana_session=aafa91a231912599e076c91f8cf7281d") {
                    "There should be a cookie in the request's headers (not ${cookie})"
                }
            }
            assert(testDispatcher.namesDispatched.size == 2) {
                "There should be 2 successful calls (not ${testDispatcher.namesDispatched.size})"
            }
            assert(testDispatcher.namesDispatched[0] == "login") {
                "The first call should be a login request (not ${testDispatcher.namesDispatched[0]})"
            }
            assert(testDispatcher.namesDispatched[1] == "folders") {
                "The second call should be the folders list (not ${testDispatcher.namesDispatched[0]})"
            }
        }
    }

    private val dispatchletLogin = Dispatchlet("login")
        .testRequest { recordedRequest ->
            recordedRequest.path == "/login" && recordedRequest.method == "POST" && recordedRequest.body.buffer.peek().readUtf8() == """{"user":"barnabo","password":"Barnabo67"}"""
        }
        .responseGetter {
            MockResponse()
                .setResponseCode(200)
                .setHeader("set-cookie", "grafana_session=aafa91a231912599e076c91f8cf7281d; Path=/; Max-Age=2592000; HttpOnly; SameSite=Lax")
                .setBody("""{"message":"Logged in","redirectUrl":"/"}""")
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