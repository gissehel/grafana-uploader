Grafana uploader is a kotlin lib to upload dashboards to a grafana instance.

# Links

* **Sources**: https://github.com/gissehel/grafana-uploader/
* **Maven Central**: https://central.sonatype.com/artifact/io.github.gissehel.grafana/grafana-uploader/overview
* **Example project using this lib**: https://github.com/gissehel/grafana-uploader-example/

# Usage

## kotlin gradle
```kotlin
implementation("io.github.gissehel.grafana:grafana-uploader:$grafana_uploader_verison")
```

## groovy gradle
```groovy
implementation 'io.github.gissehel.grafana:grafana-uploader:$grafana_uploader_verison'
```

## pom maven
```xml
<dependency>
    <groupId>io.github.gissehel.grafana</groupId>
    <artifactId>grafana-uploader</artifactId>
    <version>${grafana_uploader_verison}</version>
</dependency>
```

## In sources

Note: API may change from one version to another without warning before 1.0.0

```kotlin
import io.github.gissehel.grafana.`grafana-uploader`.GrafanaClient
import io.github.gissehel.grafana.`grafana-uploader`.model.Credential

// ...

fun uploadDashboards() {
    // Get the json representation as String of your dashboard.
    // You can use resources, or create them using the kotlin
    // grafana dsl lib you prefer.
    val dashboard1: String = getDashboard1()
    val dashboard2: String = getDashboard2()

    // A service account token
    val token = System.getenv("GRAFANA_TOKEN") ?: "V2FpdCwgZ3JhZmFuYSB0b2tlbnMgYXJlbid0IGV2ZW4gaW4gYmFzZTY0ICEhCg=="

    val grafanaClient = GrafanaClient("https://grafana.example.com", Credential.usingToken(token))

    // Upload dashboard1 at the root
    grafanaClient.createOrUpdateDashboard(dashboard = dashboard1, folderVuid = "")

    // Create a folder "Test Folder" at the root (with vuid "test-folder")
    grafanaClient.createFolderIfNotExists(vuid = "test-folder", title = "Test Folder", parentVuid = "")

    // Create a folder "Sub Test Folder" under it (with vuid "sub-test-folder")
    grafanaClient.createFolderIfNotExists(vuid = "sub-test-folder", title = "Sub Test Folder", parentVuid = "test-folder")

    // Upload dashboard2 in "Sub Test Folder"
    grafanaClient.createOrUpdateDashboard(dashboard = dashboard2, folderVuid = "sub-test-folder")
}

```

### Using username/password instead of token

If for some reason, you want to use username/password instead of Service Account's token (for example, your organisation who is managing your grafana instance doesn't want to provide you with a user account token for security reasons), you must:

* Fully understand that it's not intended to use login/password
* Fully understand that it may lead to security issues
* Opt-in for `IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible`

```kotlin
import io.github.gissehel.grafana.`grafana-uploader`.GrafanaClient
import io.github.gissehel.grafana.`grafana-uploader`.model.Credential
import io.github.gissehel.grafana.`grafana-uploader`.model.credential.IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible

// ...

@OptIn(IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible::class)
fun uploadDashboards() {
    // Get the json representation as String of your dashboard.
    // You can use resources, or create them using the kotlin
    // grafana dsl lib you prefer.
    val dashboard1: String = getDashboard1()
    val dashboard2: String = getDashboard2()

    // A username/password
    val username = System.getenv("GRAFANA_USERNAME") ?: "barnabo"
    val password = System.getenv("GRAFANA_PASSWORD") ?: "************"

    val grafanaClient = GrafanaClient("https://grafana.example.com", Credential.usingGrafanaUsernamePassword(username, password))

    // Upload dashboard1 at the root
    grafanaClient.createOrUpdateDashboard(dashboard = dashboard1, folderVuid = "")

    // Create a folder "Test Folder" at the root (with vuid "test-folder")
    grafanaClient.createFolderIfNotExists(vuid = "test-folder", title = "Test Folder", parentVuid = "")

    // Create a folder "Sub Test Folder" under it (with vuid "sub-test-folder")
    grafanaClient.createFolderIfNotExists(vuid = "sub-test-folder", title = "Sub Test Folder", parentVuid = "test-folder")

    // Upload dashboard2 in "Sub Test Folder"
    grafanaClient.createOrUpdateDashboard(dashboard = dashboard2, folderVuid = "sub-test-folder")
}

```



# Other informations

First layout of this project inspired by https://github.com/Kotlin/multiplatform-library-template