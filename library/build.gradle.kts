import com.vanniktech.maven.publish.SonatypeHost
import java.util.Properties

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
    file(filename).outputStream().use { properties.store(it, null) }
}

fun deleteProperties(filename: String) {
    file(filename).delete()
}

fun String.getIntOrDefault(default: Int): Int {
    return try {
        this.toInt()
    } catch (e: NumberFormatException) {
        default
    }
}

fun String.getBooleanOrDefault(default: Boolean): Boolean {
    return try {
        this.toBoolean()
    } catch (e: NumberFormatException) {
        default
    }
}

fun Properties.getInt(key: String, default: Int): Int = (this[key] as String?)?.getIntOrDefault(default) ?: default
fun Properties.getBoolean(key: String, default: Boolean): Boolean = (this[key] as String?)?.getBooleanOrDefault(default) ?: default


fun Version.asVersionString(): String {
    val contextSuffix = if (this.context != null) "-${this.context}" else ""
    val snapshotSuffix = if (this.preRelease) "-SNAPSHOT" else ""
    return "${this.major}.${this.minor}.${this.patch}${contextSuffix}${snapshotSuffix}"
}

fun Version.getReleaseVersion(): Version {
    return Version(
        major = this.major,
        minor = this.minor,
        patch = this.patch,
        preRelease = false,
        context = this.context,
    )
}

fun Version.getNextPatchVersion(): Version {
    return Version(
        major = this.major,
        minor = this.minor,
        patch = this.patch+1,
        preRelease = true,
        context = this.context,
    )
}

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

tasks.register("setVersion") {
    group = "versioning"
    description = "Set the version"
    val major = project.findProperty("major") as String? ?: throw IllegalArgumentException("You must provide an integer major")
    val minor = project.findProperty("minor") as String? ?: throw IllegalArgumentException("You must provide an integer minor")
    val patch = project.findProperty("patch") as String? ?: throw IllegalArgumentException("You must provide an integer patch")
    val isSnapshot = project.findProperty("isSnapshot") as String? ?: throw IllegalArgumentException("You must provide a boolean isSnapshot")
    val newVersion = Version(major.getIntOrDefault(0), minor.getIntOrDefault(0), patch.getIntOrDefault(0), preRelease = isSnapshot.getBooleanOrDefault(true))
    val newVersionString = newVersion.asVersionString()
    writeVersion(newVersion)
    doLast {
        println("Version updated to ${newVersionString}")
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

tasks.register("releaseVersion") {
    group = "versioning"
    description = "Switch from a snapshot version to a release version"
    val oldVersion = readVersion()
    val newVersion = oldVersion.getReleaseVersion()
    val newVersionString = newVersion.asVersionString()
    writeVersion(newVersion)
    doLast {
        println("Version updated to ${newVersionString}")
    }
}

tasks.register("setNextPrereleaseVersion") {
    group = "versioning"
    description = "Switch from a release version to the next prerelease version"
    val oldVersion = readVersion()
    val newVersion = oldVersion.getNextPatchVersion()
    val newVersionString = newVersion.asVersionString()
    writeVersion(newVersion)
    doLast {
        println("Version updated to ${newVersionString}")
    }
}

tasks.register("getVersion") {
    group = "versioning"
    description = "get current version"
    val current_version = version
    doLast {
        println("Version: ${current_version}")
    }
}
