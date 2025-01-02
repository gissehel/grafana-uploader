import com.vanniktech.maven.publish.SonatypeHost
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.*

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

val currentVersion = readVersion().asVersionString()

group = "io.github.gissehel.grafana"
version = currentVersion

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

//region Verisoning
class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: Boolean = false,
    val context: String? = null,
)

fun readProperties(filename: String): Properties {
    val properties = Properties()
    file(filename).apply {
        if (exists()) inputStream().use { properties.load(it) }
    }
    return properties
}

fun writeProperties(properties: Properties, filename: String) {
    // For some reason, the following line output a Date in comment, and it's not best when using git
    // file(filename).outputStream().use { properties.store(it, null) }

    with(BufferedWriter(OutputStreamWriter(file(filename).outputStream(), Charsets.UTF_8))) {
        synchronized(this) {
            for(key in properties.keys().toList().map{ it.toString() }.sorted()) {
                write("${key}=${properties[key]}")
                // Not newLine() because there no reason a property file ends up with CRLF under windows, while
                // everyone use LF even on Windows for code
                write("\n")
            }
        }
        flush()
    }
}

fun deleteProperties(filename: String) {
    file(filename).delete()
}

fun <T> getResultOrDefault(data: String, default: T, block: (String)->T): T {
    return try {
        block(data)
    } catch (e: NumberFormatException) {
        default
    }
}
fun String.getIntOrDefault(default: Int): Int = getResultOrDefault(this, default) { this.toInt() }
fun String.getBooleanOrDefault(default: Boolean): Boolean = getResultOrDefault(this, default) { this.toBoolean() }

fun Properties.getInt(key: String, default: Int): Int = (this[key] as String?)?.getIntOrDefault(default) ?: default
fun Properties.getBoolean(key: String, default: Boolean): Boolean = (this[key] as String?)?.getBooleanOrDefault(default) ?: default

fun Version.asVersionString(): String {
    val contextSuffix = if (this.context != null) "-${this.context}" else ""
    val snapshotSuffix = if (this.preRelease) "-SNAPSHOT" else ""
    return "${this.major}.${this.minor}.${this.patch}${contextSuffix}${snapshotSuffix}"
}

fun Version.getReleaseVersion(): Version = Version(this.major, this.minor, this.patch,false,this.context)
fun Version.getNextPatchVersion(): Version = Version(this.major, this.minor, this.patch+1, true, this.context)
fun Version.getNextMinorVersion(): Version = Version(this.major, this.minor+1, 0, true, this.context)
fun Version.getNextMajorVersion(): Version = Version(this.major+1, 0, 0, true, this.context)

fun readVersion(): Version {
    val properties = readProperties("version.properties")
    val contextProperties = readProperties("version-context.properties")

    val version = Version(
        major = properties.getInt("major",0),
        minor = properties.getInt("minor", 0),
        patch = properties.getInt("patch", 0),
        preRelease = properties.getBoolean("isSnapshot", false),
        context = contextProperties["context"] as String?
    )

    return version
}

fun updateProperties(filename: String, entries: Map<String, String>) {
    val properties = readProperties("version-context.properties")
    entries.keys.forEach { key ->
        properties[key] = entries[key]
    }
    writeProperties(properties, filename)
}

fun writeVersionContext(versionContext: String?) {
    if (versionContext == null) {
        deleteProperties("version-context.properties")
    } else {
        updateProperties("version-context.properties", mapOf(
            "context" to versionContext,
        ))
    }
}

fun writeVersion(version: Version) {
    updateProperties("version.properties", mapOf(
        "major" to version.major.toString(),
        "minor" to version.minor.toString(),
        "patch" to version.patch.toString(),
        "isSnapshot" to version.preRelease.toString()
    ))
    writeVersionContext(version.context)
}

tasks.register("getVersion") {
    group = "versioning"
    description = "get current version"
    val currentVersion = version
    doLast {
        println("Version: ${currentVersion}")
    }
}

tasks.register("setVersionContext") {
    group = "versioning"
    description = "Set the version context"
    val context = project.findProperty("context") as String?
    writeVersionContext(context)
    doLast {
        println("Version context updated to [${context ?: ""}]")
    }
}

private data class TaskParams(
    val name: String,
    val description: String,
    val block: Task.() -> Version
)

listOf(
    TaskParams("setVersion","Set the version") {
        val major = project.findProperty("major") as String? ?: throw IllegalArgumentException("You must provide an integer major")
        val minor = project.findProperty("minor") as String? ?: throw IllegalArgumentException("You must provide an integer minor")
        val patch = project.findProperty("patch") as String? ?: throw IllegalArgumentException("You must provide an integer patch")
        val isSnapshot = project.findProperty("isSnapshot") as String? ?: throw IllegalArgumentException("You must provide a boolean isSnapshot")
        Version(
            major.getIntOrDefault(0),
            minor.getIntOrDefault(0),
            patch.getIntOrDefault(0),
            preRelease = isSnapshot.getBooleanOrDefault(true)
        )
    },
    TaskParams("releaseVersion", "Switch from a snapshot version to a release version") {
        readVersion().getReleaseVersion()
    },
    TaskParams("setNextPrereleaseVersion", "Switch from a release version to the next prerelease version") {
        readVersion().getNextPatchVersion()
    },
    TaskParams("setNextMinorPrereleaseVersion", "Switch the next minor prerelease version") {
        readVersion().getNextMinorVersion()
    },
    TaskParams("setNextMajorPrereleaseVersion", "Switch the next major prerelease version") {
        readVersion().getNextMajorVersion()
    },
).forEach { taskParams ->
    tasks.register(taskParams.name) {
        group = "versioning"
        description = taskParams.description
        val newVersion = taskParams.block(this)
        val newVersionString = newVersion.asVersionString()
        writeVersion(newVersion)
        doLast {
            println("Version updated to ${newVersionString}")
        }
    }
}
//endregion Verisoning