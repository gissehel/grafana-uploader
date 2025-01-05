package io.github.gissehel.grafana.grafanauploader

import io.github.gissehel.grafana.grafanauploader.error.CommunicationError
import io.github.gissehel.grafana.grafanauploader.model.Credential
import io.github.gissehel.grafana.grafanauploader.tools.*
import io.github.gissehel.grafana.grafanauploader.tools.model.Dispatchlet
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
    fun createGrafanaClient() = GrafanaClient(getRootUrl(), Credential.usingToken("grut"))
    fun createGrafanaClientAndDebug() = GrafanaClient(getRootUrl(), Credential.usingToken("grut")) {
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
            "There should be only one successful call (not ${testDispatcher.namesDispatched.size})"
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
    fun `createOrUpdateDashboard should create a new diagram with null id and uid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletDiagramCreateNullIdUid)

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
        mockServer.takeRequest().let { request ->
            val jsonElement = Json.parseToJsonElement(request.body.buffer.peek().readUtf8())
            val requestObject = jsonElement.asObject()
            assert(requestObject.containsKey("dashboard")) {
                "Request should contains a dashboard"
            }
            val requestDashboard = requestObject["dashboard"]!!.asObject()
            assert(requestDashboard.containsKey("id")) {
                "Request dashboard should contains an id"
            }
            val requestDashboardId = requestDashboard["id"]!!
            assert(requestDashboardId.asNull() == "null") {
                "Request dashboard id should be null"
            }
            assert(requestDashboard.containsKey("uid")) {
                "Request dashboard should contains an uid"
            }
            val requestDashboardUid = requestDashboard["uid"]!!
            assert(requestDashboardUid.asNull() == "null") {
                "Request dashboard uid should be null"
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `createOrUpdateDashboard should create a new diagram with an id and a uid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletDiagramCreateNonNullIdUid)

        val dashboard = buildJsonObject {
            put("id", 27)
            put("uid", "dummy-dashboard")
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
        mockServer.takeRequest().let { request ->
            val jsonElement = Json.parseToJsonElement(request.body.buffer.peek().readUtf8())
            val requestObject = jsonElement.asObject()
            assert(requestObject.containsKey("dashboard")) {
                "Request should contains a dashboard"
            }
            val requestDashboard = requestObject["dashboard"]!!.asObject()
            assert(requestDashboard.containsKey("id")) {
                "Request dashboard should contains an id"
            }
            val requestDashboardId = requestDashboard["id"]!!
            assert(requestDashboardId.asInteger() == 27) {
                "Request dashboard id should be 27 (not ${requestDashboardId.asInteger()})"
            }
            assert(requestDashboard.containsKey("uid")) {
                "Request dashboard should contains an uid"
            }
            val requestDashboardUid = requestDashboard["uid"]!!
            assert(requestDashboardUid.asString() == "dummy-dashboard") {
                "Request dashboard uid should be \"dummy-dashboard\" (not \"${requestDashboardUid.asString()}\")"
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `createOrUpdateDashboard should create a new diagram with null id and uid and forced vuid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletDiagramCreateNullIdUidForcedVuid)

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

        grafanaClient.createOrUpdateDashboard("forced-dashboard", dashboard, "test-folder")

        assert(mockServer.requestCount == 1) {
            "It should have created only 1 request (not ${mockServer.requestCount})"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call (not ${testDispatcher.namesDispatched.size})"
        }
        assert(testDispatcher.namesDispatched[0] == "createDiagram") {
            "The only call should be the diagram creation (not ${testDispatcher.namesDispatched[0]})"
        }
        mockServer.takeRequest().let { request ->
            val jsonElement = Json.parseToJsonElement(request.body.buffer.peek().readUtf8())
            val requestObject = jsonElement.asObject()
            assert(requestObject.containsKey("dashboard")) {
                "Request should contains a dashboard"
            }
            val requestDashboard = requestObject["dashboard"]!!.asObject()
            assert(requestDashboard.containsKey("id")) {
                "Request dashboard should contains an id"
            }
            val requestDashboardId = requestDashboard["id"]!!
            assert(requestDashboardId.asNull() == "null") {
                "Request dashboard id should be null"
            }
            assert(requestDashboard.containsKey("uid")) {
                "Request dashboard should contains an uid"
            }
            val requestDashboardUid = requestDashboard["uid"]!!
            assert(requestDashboardUid.asString() == "forced-dashboard") {
                "Request dashboard uid should be \"forced-dashboard\" (not \"${requestDashboardUid.asString()}\")"
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `createOrUpdateDashboard should create a new diagram with an id and a uid and forced vuid`() {
        val grafanaClient = createGrafanaClient()

        testDispatcher.add(dispatchletDiagramCreateNonNullIdUidForcedVuid)

        val dashboard = buildJsonObject {
            put("id", 27)
            put("uid", "dummy-dashboard")
            put("title", "Production Overview")
            put("tags", buildJsonArray {
                add("templated")
            })
            put("timezone", "browser")
            put("schemaVersion", 16)
            put("refresh", "25s")
        }.toString()

        grafanaClient.createOrUpdateDashboard("forced-dashboard", dashboard, "test-folder")

        assert(mockServer.requestCount == 1) {
            "It should have created only 1 request (not ${mockServer.requestCount})"
        }
        assert(testDispatcher.namesDispatched.size == 1) {
            "There should be only one successful call (not ${testDispatcher.namesDispatched.size})"
        }
        assert(testDispatcher.namesDispatched[0] == "createDiagram") {
            "The only call should be the diagram creation (not ${testDispatcher.namesDispatched[0]})"
        }
        mockServer.takeRequest().let { request ->
            val jsonElement = Json.parseToJsonElement(request.body.buffer.peek().readUtf8())
            val requestObject = jsonElement.asObject()
            assert(requestObject.containsKey("dashboard")) {
                "Request should contains a dashboard"
            }
            val requestDashboard = requestObject["dashboard"]!!.asObject()
            assert(requestDashboard.containsKey("id")) {
                "Request dashboard should contains an id"
            }
            val requestDashboardId = requestDashboard["id"]!!
            assert(requestDashboardId.asNull() == "null") {
                "Request dashboard id should be null"
            }
            assert(requestDashboard.containsKey("uid")) {
                "Request dashboard should contains an uid"
            }
            val requestDashboardUid = requestDashboard["uid"]!!
            assert(requestDashboardUid.asString() == "forced-dashboard") {
                "Request dashboard uid should be \"forced-dashboard\" (not \"${requestDashboardUid.asString()}\")"
            }
        }
    }


    private val staticDiagramProps = """"title":"Production Overview","tags":["templated"],"timezone":"browser","schemaVersion":16,"refresh":"25s""""
    private val staticDiagramBaseNullIdUid = """{"id":null,"uid":null,${staticDiagramProps}}"""
    private val staticDiagramBaseNonNullIdUid = """{"id":27,"uid":"dummy-dashboard",${staticDiagramProps}}"""
    private val staticDiagramBaseNullIdUidForcedVuid = """{"id":null,"uid":"forced-dashboard",${staticDiagramProps}}"""
    private val staticDiagramBaseNonNullIdUidForcedVuid = """{"id":null,"uid":"forced-dashboard",${staticDiagramProps}}"""
    private val staticDiagramRequestCreationNullIdUid = """{"dashboard":${staticDiagramBaseNullIdUid},"folderUid":"test-folder","message":"autogenerated","overwrite":true}"""
    private val staticDiagramRequestCreationNonNullIdUid = """{"dashboard":${staticDiagramBaseNonNullIdUid},"folderUid":"test-folder","message":"autogenerated","overwrite":true}"""
    private val staticDiagramRequestCreationNullIdUidForcedVuid = """{"dashboard":${staticDiagramBaseNullIdUidForcedVuid},"folderUid":"test-folder","message":"autogenerated","overwrite":true}"""
    private val staticDiagramRequestCreationNonNullIdUidForcedVuid = """{"dashboard":${staticDiagramBaseNonNullIdUidForcedVuid},"folderUid":"test-folder","message":"autogenerated","overwrite":true}"""
    private val staticDiagramCreatedNullIdUid = """{"id":73,"uid":"e883f11b-77c0-4ee3-9a70-3ba223d66e56","url":"/d/e883f11b-77c0-4ee3-9a70-3ba223d66e56/production-overview","status":"success","version":2,"slug":"production-overview"}"""
    private val staticDiagramCreatedNonNullIdUid = """{"id":27,"uid":"dummy-dashboard","url":"/d/dummy-dashboard/production-overview","status":"success","version":2,"slug":"production-overview"}"""
    private val staticDiagramCreatedNullIdUidForcedVuid = """{"id":73,"uid":"forced-dashboard","url":"/d/forced-dashboard/production-overview","status":"success","version":2,"slug":"production-overview"}"""
    private val staticDiagramCreatedNonNullIdUidForcedVuid = """{"id":73,"uid":"forced-dashboard","url":"/d/forced-dashboard/production-overview","status":"success","version":2,"slug":"production-overview"}"""

    private val dispatchletDiagramCreateNullIdUid = createPostDispatchlet("createDiagram", "/api/dashboards/db", staticDiagramRequestCreationNullIdUid, staticDiagramCreatedNullIdUid)
    private val dispatchletDiagramCreateNonNullIdUid = createPostDispatchlet("createDiagram", "/api/dashboards/db", staticDiagramRequestCreationNonNullIdUid, staticDiagramCreatedNonNullIdUid)
    private val dispatchletDiagramCreateNullIdUidForcedVuid = createPostDispatchlet("createDiagram", "/api/dashboards/db", staticDiagramRequestCreationNullIdUidForcedVuid, staticDiagramCreatedNullIdUidForcedVuid)
    private val dispatchletDiagramCreateNonNullIdUidForcedVuid = createPostDispatchlet("createDiagram", "/api/dashboards/db", staticDiagramRequestCreationNonNullIdUidForcedVuid, staticDiagramCreatedNonNullIdUidForcedVuid)
    //endregion
}