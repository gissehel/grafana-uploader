package io.github.gissehel.grafana.`grafana-uploader`

import io.github.gissehel.grafana.`grafana-uploader`.tools.asObject
import io.github.gissehel.grafana.`grafana-uploader`.tools.asString
import io.github.gissehel.grafana.uploader.GrafanaClient
import io.github.gissehel.grafana.uploader.error.CommunicationError
import io.github.gissehel.grafana.uploader.tools.TestDispatcher
import io.github.gissehel.grafana.uploader.tools.model.Dispatchlet
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GrafanaClientTest {
    val testDispatcher = TestDispatcher()
    val mockServer = MockWebServer().apply {
        dispatcher = testDispatcher
        start()
    }
    fun getRootUrl() : String = mockServer.url("").toString().dropLast(1) // url like "http://localhost:9999"
    fun createGrafanaClient() = GrafanaClient("grut", getRootUrl())
    fun createGrafanaClientAndDebug() = GrafanaClient("grut", getRootUrl()) {
        println(it)
    }

    fun createGetDispatchlet(name: String, path: String, body: String) = Dispatchlet(name)
        .testRequest { recordedRequest -> recordedRequest.path == path && recordedRequest.method == "GET" }
        .responseGetter { MockResponse().setResponseCode(200).setBody(body) }

    fun createPostDispatchlet(name: String, path: String, requestBody: String, body: String) = Dispatchlet(name)
        .testRequest { recordedRequest -> recordedRequest.path == path && recordedRequest.method == "POST" &&  recordedRequest.body.buffer.peek().readUtf8() == requestBody }
        .responseGetter { MockResponse().setResponseCode(200).setBody(body) }


    @BeforeEach
    fun beforeEach() {
        testDispatcher.clear()
    }

    //region folders
    @Test
    fun `createFolderIfNotExists on a server that doesn't act like grafana should fail`() {
        val grafanaClient = createGrafanaClient()

        assertFailsWith<CommunicationError> {
            grafanaClient.createFolderIfNotExists("test-folder-foo", "Foo", "test-folder")
            assert(false) {
                "Should not arrive here"
            }
        }
        assert(testDispatcher.namesDispatched.size == 0) {
            "There should be no successful call"
        }

    }

    @Test
    fun `createFolderIfNotExists should try to create a folder on a server that act like grafana`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletFoldersWhenEmpty)
        testDispatcher.add(dispatchletFolderAddFoo)

        grafanaClient.createFolderIfNotExists("test-folder-foo", "Foo", "test-folder")
        assert(mockServer.requestCount == 2) {
            "It should have created 2 requests"
        }
        mockServer.takeRequest()
        val request = mockServer.takeRequest()
        val bodyContent = request.body.buffer.readUtf8()
        val element = Json.parseToJsonElement(bodyContent)

        assert(element.asObject().containsKey("uid")) {
            "Result should have a uid"
        }
        assert(element.asObject()["uid"]?.asString() == "test-folder-foo") {
            "Result should have a uid with value test-folder-foo (not ${element.asObject()["uid"]?.asString()})"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call"
        }
        assert(testDispatcher.namesDispatched[0] == "folderAddFoo") {
            "The only call should be the creation of folder foo (not ${testDispatcher.namesDispatched[0]})"
        }
    }

    @Test
    fun `createFolderIfNotExists should not try to create a folder if it already exists`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletFoldersWhenEmpty)
        testDispatcher.add(dispatchletFolderAddFoo)
        testDispatcher.add(dispatchletTestIfFolderFooExistsWhenFooExists)

        grafanaClient.createFolderIfNotExists("test-folder-foo", "Foo", "test-folder")
        assert(mockServer.requestCount == 1) {
            "It should have created only 1 request"
        }

        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call"
        }
        assert(testDispatcher.namesDispatched[0] == "folderFoo") {
            "The only call should be the existence of folder foo (not ${testDispatcher.namesDispatched[0]})"
        }
    }

    @Test
    fun `createFolderIfNotExists should try to create a folder on a server that act like grafana even with a very long vuid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletFoldersWhenEmpty)
        testDispatcher.add(dispatchletFolderAddBar)

        grafanaClient.createFolderIfNotExists("this-is-a-long-name-with-length-that-is-42", "Very long uid", "test-folder")
        assert(mockServer.requestCount == 2) {
            "It should have created 2 requests"
        }
        mockServer.takeRequest()
        val request = mockServer.takeRequest()
        val bodyContent = request.body.buffer.readUtf8()
        val element = Json.parseToJsonElement(bodyContent)

        assert(element.asObject().containsKey("uid")) {
            "Result should have a uid"
        }
        assert(element.asObject()["uid"]?.asString() == "24ddb5032c4ba854a9c5319c983444d5bd9ea1e4") {
            "Result should have a uid with value 24ddb5032c4ba854a9c5319c983444d5bd9ea1e4"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call"
        }
        assert(testDispatcher.namesDispatched[0] == "folderAddBar") {
            "The only call should be the creation of folder bar (not ${testDispatcher.namesDispatched[0]})"
        }
    }

    @Test
    fun `createFolderIfNotExists should not try to create a folder if it already exists even with a very long vuid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletFoldersWhenEmpty)
        testDispatcher.add(dispatchletFolderAddBar)
        testDispatcher.add(dispatchletTestIfFolderBarExistsWhenBarExists)

        grafanaClient.createFolderIfNotExists("this-is-a-long-name-with-length-that-is-42", "Very long uid", "test-folder")
        assert(mockServer.requestCount == 1) {
            "It should have created only 1 request"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call"
        }
        assert(testDispatcher.namesDispatched[0] == "folderBar") {
            "The only call should be the existence of folder bar (not ${testDispatcher.namesDispatched[0]})"
        }
    }


    @Test
    fun `createFolderIfNotExists should try to create a folder on a server that act like grafana even if the parent has a very long vuid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletFoldersWhenBarExists)
        testDispatcher.add(dispatchletFolderAddShon)

        grafanaClient.createFolderIfNotExists("this-is-a-long-name-with-length-value-40", "Almost very long uid", "this-is-a-long-name-with-length-that-is-42")
        assert(mockServer.requestCount == 2) {
            "It should have created 2 requests"
        }
        mockServer.takeRequest()
        val request = mockServer.takeRequest()
        val bodyContent = request.body.buffer.readUtf8()
        val element = Json.parseToJsonElement(bodyContent)

        assert(element.asObject().containsKey("uid")) {
            "Result should have a uid"
        }
        assert(element.asObject()["uid"]?.asString() == "this-is-a-long-name-with-length-value-40") {
            "Result should have a uid with value this-is-a-long-name-with-length-value-40"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call"
        }
        assert(testDispatcher.namesDispatched[0] == "folderAddShon") {
            "The only call should be the creation of folder shon (not ${testDispatcher.namesDispatched[0]})"
        }
    }

    @Test
    fun `createFolderIfNotExists should not try to create a folder if it already exists even if the parent has a very long vuid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletFoldersWhenBarExists)
        testDispatcher.add(dispatchletFolderAddShon)
        testDispatcher.add(dispatchletTestIfFolderShonExistsWhenShonExists)

        grafanaClient.createFolderIfNotExists("this-is-a-long-name-with-length-value-40", "Almost very long uid", "this-is-a-long-name-with-length-that-is-42")
        assert(mockServer.requestCount == 1) {
            "It should have created only 1 request"
        }

        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call"
        }
        assert(testDispatcher.namesDispatched[0] == "folderShon") {
            "The only call should be the existence of folder shon (not ${testDispatcher.namesDispatched[0]})"
        }
    }

    private val staticFoldersBase = """{"id":32,"uid":"test-folder","title":"Test folder"},{"id":33,"uid":"dummy","title":"Dummy folder"}"""
    private val staticFoldersWhenEmpty = """[${staticFoldersBase}]"""
    private val staticFolderFooCreated = """{"id":35,"uid":"test-folder-foo","title":"Foo","parentUid":"test-folder"}"""
    private val staticFolderFooRequest = """{"uid":"test-folder-foo","title":"Foo","parentUid":"test-folder"}"""
    private val staticFolderBarCreated = """{"id":36,"uid":"24ddb5032c4ba854a9c5319c983444d5bd9ea1e4","title":"Very long uid","parentUid":"test-folder"}"""
    private val staticFolderBarRequest = """{"uid":"24ddb5032c4ba854a9c5319c983444d5bd9ea1e4","title":"Very long uid","parentUid":"test-folder"}"""
    private val staticFolderShonCreated = """{"id":37,"uid":"this-is-a-long-name-with-length-value-40","title":"Almost very long uid","parentUid":"24ddb5032c4ba854a9c5319c983444d5bd9ea1e4"}"""
    private val staticFolderShonRequest = """{"uid":"this-is-a-long-name-with-length-value-40","title":"Almost very long uid","parentUid":"24ddb5032c4ba854a9c5319c983444d5bd9ea1e4"}"""
    // private val staticFoldersWhenFooExists = """[${staticFoldersBase},${staticFolderFooCreated}]"""
    private val staticFoldersWhenBarExists = """[${staticFoldersBase},${staticFolderBarCreated}]"""

    private val dispatchletFoldersWhenEmpty = createGetDispatchlet("folders", "/api/folders", staticFoldersWhenEmpty)
    // private val dispatchletFoldersWhenFooExists = createGetDispatchlet("folders", "/api/folders", staticFoldersWhenFooExists)
    private val dispatchletTestIfFolderFooExistsWhenFooExists = createGetDispatchlet("folderFoo", "/api/folders/test-folder-foo", staticFolderFooCreated)
    private val dispatchletTestIfFolderBarExistsWhenBarExists = createGetDispatchlet("folderBar", "/api/folders/24ddb5032c4ba854a9c5319c983444d5bd9ea1e4", staticFolderBarCreated)
    private val dispatchletTestIfFolderShonExistsWhenShonExists = createGetDispatchlet("folderShon", "/api/folders/this-is-a-long-name-with-length-value-40", staticFolderShonCreated)
    private val dispatchletFoldersWhenBarExists = createGetDispatchlet("folders", "/api/folders", staticFoldersWhenBarExists)
    private val dispatchletFolderAddFoo = createPostDispatchlet("folderAddFoo", "/api/folders", staticFolderFooRequest, staticFolderFooCreated)
    private val dispatchletFolderAddBar = createPostDispatchlet("folderAddBar", "/api/folders", staticFolderBarRequest, staticFolderBarCreated)
    private val dispatchletFolderAddShon = createPostDispatchlet("folderAddShon", "/api/folders", staticFolderShonRequest, staticFolderShonCreated)

    //endregion folders

    //region dashboards
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `should create a new diagram`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletDiagramCreate)

        val dashboard = buildJsonObject {
            put("id", null)
            put("uid", null)
            put("title", "Production Overview")
            put("tags", buildJsonArray {
                add("templated")
            })
            put("timezone", "browser")
            put("schemaVersion", 16)
            put("refresh", "25s")
        }.toString()

        grafanaClient.createOrUpdateDashboard(dashboard, "test-folder")

        assert(mockServer.requestCount == 1) {
            "It should have created only 1 request (not ${mockServer.requestCount})"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call (not ${testDispatcher.namesDispatched.size})"
        }
        assert(testDispatcher.namesDispatched[0] == "createDiagram") {
            "The only call should be the diagram creation (not ${testDispatcher.namesDispatched[0]})"
        }
    }
    private val staticDiagramProps = """"title":"Production Overview","tags":["templated"],"timezone":"browser","schemaVersion":16,"refresh":"25s""""
    private val staticDiagramBase = """{"id":null,"uid":null,${staticDiagramProps}}"""
    private val staticDiagramRequestCreation = """{"dashboard":${staticDiagramBase},"folderUid":"test-folder","message":"autogenerated","overwrite":true}"""
    private val staticDiagramCreated = """{"id":73,"uid":"e883f11b-77c0-4ee3-9a70-3ba223d66e56","url":"/d/e883f11b-77c0-4ee3-9a70-3ba223d66e56/production-overview","status":"success","version":2,"slug":"production-overview"}"""

    private val dispatchletDiagramCreate = createPostDispatchlet("createDiagram", "/api/dashboards/db", staticDiagramRequestCreation, staticDiagramCreated)
    //endregion
}