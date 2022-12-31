// Adapted from <https://github.com/JetBrains/intellij-platform-plugin-template>.

import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()


plugins {
    kotlin("jvm") version("1.7.21")
    id("org.jetbrains.intellij") version("1.11.0")
    id("org.jetbrains.changelog") version("1.3.1")
}


kotlin {
    sourceSets {
        val vendor by creating {
            kotlin.srcDir("src/vendor")
        }
        getByName("main") {
            dependsOn(vendor)
        }
    }
}


intellij {
    // <https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html>
    version.set(properties("platformVersion"))
    updateSinceUntilBuild.set(true)
    downloadSources.set(true)
}


changelog {
    // <https://github.com/JetBrains/gradle-changelog-plugin>
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("-")
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}


repositories {
    mavenCentral()
    maven {
        url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    }
}


dependencies {
    val netcdfVersion = "5.4.2"
    implementation("edu.ucar:cdm-core:${netcdfVersion}")
    runtimeOnly("edu.ucar:netcdf4:${netcdfVersion}")
    runtimeOnly("org.slf4j:slf4j-jdk14:2.0.0")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")  // required for IDE platform tests
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.0")  // JUnit test runner
}


tasks {

    wrapper {
        gradleVersion = "7.5.1"
    }

    patchPluginXml {
        // https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-patchpluginxml
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            File(projectDir, "README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(changelog.getLatest().toHTML())
    }

    runPluginVerifier {
        // https://github.com/JetBrains/intellij-plugin-verifier
        ideVersions.set(properties("pluginVerifyVersions").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    runIdeForUiTests {
        // TODO
        // <https://github.com/JetBrains/intellij-ui-test-robot>
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    check {
        // Add plugin validation tasks to default checks.
        dependsOn(verifyPlugin)
        dependsOn(verifyPluginConfiguration)
        dependsOn(runPluginVerifier)
    }

    test {
        useJUnitPlatform()  // use JUnit5
    }
}
