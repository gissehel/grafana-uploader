import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.gissehel.grafana"
version = "0.0.2-SNAPSHOT"

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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

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
