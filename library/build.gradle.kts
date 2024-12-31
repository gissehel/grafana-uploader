import com.vanniktech.maven.publish.SonatypeHost
import java.util.Properties

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.gissehel.grafana"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin{
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    signAllPublications()

    coordinates(group.toString(), "grafana-uploader", version.toString())

    pom {
        name.set("grafana-uploader")
        description.set("Grafana uploader library")
        inceptionYear.set("2025")
        url.set("https://github.com/gissehel/grafana-uploader/")
        licenses {
            license {
                name.set("MIT")
                url.set("https://mit-license.org/")
                distribution.set("https://mit-license.org/")
            }
        }
        developers {
            developer {
                id.set("gissehel")
                name.set("Gissehel")
                url.set("https://github.com/gissehel/")
                email.set("gissehel@users.noreply.github.com")
            }
        }
        scm {
            url.set("https://github.com/gissehel/grafana-uploader/")
            connection.set("scm:git:git://github.com/gissehel/grafana-uploader.git")
            developerConnection.set("scm:git:ssh://git@github.com/gissehel/grafana-uploader.git")
        }
    }
}


// Fonction pour lire la version actuelle depuis un fichier version.properties
fun readVersion(): String {
    val properties = Properties()
    file("version.properties").inputStream().use { properties.load(it) }
    return properties.getProperty("version")
}

// Fonction pour écrire une nouvelle version dans le fichier version.properties
fun writeVersion(version: String) {
    val properties = Properties()
    file("version.properties").apply {
        if (exists()) inputStream().use { properties.load(it) }
    }
    properties["version"] = version
    file("version.properties").outputStream().use { properties.store(it, null) }
}

// Définir la version actuelle
val currentVersion = readVersion()
version = currentVersion

tasks.register("setReleaseVersion") {
    group = "versioning"
    description = "Set the version to a release version"
    doLast {
        val releaseVersion = project.findProperty("releaseVersion") as String?
            ?: throw IllegalArgumentException("You must provide a releaseVersion")
        writeVersion(releaseVersion)
        println("Version updated to $releaseVersion")
    }
}

tasks.register("setNextSnapshotVersion") {
    group = "versioning"
    description = "Set the version to the next snapshot version"
    doLast {
        val nextSnapshotVersion = project.findProperty("nextSnapshotVersion") as String?
            ?: throw IllegalArgumentException("You must provide a nextSnapshotVersion")
        writeVersion(nextSnapshotVersion)
        println("Version updated to $nextSnapshotVersion")
    }
}