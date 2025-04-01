/*
 * Copyright 2022-2023 Creek Contributors (https://github.com/creek-service)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    java
    jacoco
    `creek-common-convention`
    `creek-coverage-convention`
    `creek-plugin-publishing-convention`
    `creek-sonatype-publishing-convention`
    id("com.gradle.plugin-publish")
    id("pl.allegro.tech.build.axion-release") version "1.18.18" // https://plugins.gradle.org/plugin/pl.allegro.tech.build.axion-release
}

project.version = scmVersion.version

allprojects {
    tasks.jar {
        onlyIf { sourceSets.main.get().allSource.files.isNotEmpty() }
    }
}

val creekVersion = "0.4.2-SNAPSHOT"
val spotBugsVersion = "4.9.1"         // https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
val guavaVersion = "33.4.0-jre"         // https://mvnrepository.com/artifact/com.google.guava/guava
val log4jVersion = "2.24.3"           // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
val junitVersion = "5.12.0"            // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
val junitPioneerVersion = "2.3.0"     // https://mvnrepository.com/artifact/org.junit-pioneer/junit-pioneer
val mockitoVersion = "5.15.2"          // https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter

dependencies {
    implementation("com.github.spotbugs:spotbugs-annotations:$spotBugsVersion")
    // Avoid non-test dependencies in plugins.

    testImplementation("org.creekservice:creek-test-hamcrest:$creekVersion")
    testImplementation("org.creekservice:creek-test-util:$creekVersion")
    testImplementation("org.creekservice:creek-test-conformity:$creekVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("com.google.guava:guava-testlib:$guavaVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    // The following dependency is only added to force GitHub Dependency Bot to take the executor version into account
    testRuntimeOnly("org.creekservice:creek-system-test-executor:$creekVersion")
}

gradlePlugin {
    plugins {
        register("CreekPlugin") {
            id = "org.creekservice.system.test"
            implementationClass = "org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin"
            displayName = "Creek system test plugin"
            description = "Executes blackbox testing of services running in Docker containers"
            tags.set(listOf("creek", "creekservice", "microservice", "docker", "containers", "blackbox", "testing", "test", "integration-test", "test automation"))
        }
    }
}

tasks.register("writeVersionFile") {
    val outputDir = file("$buildDir/generated/resources/version")
    val versionFile = file("$outputDir/creek-system-test-executor.version")
    sourceSets.main.get().output.dir(mapOf("buildBy" to "writeVersionFile"), outputDir)

    inputs.property("executorVersion", creekVersion)
    outputs.dir(outputDir).withPropertyName("outputDir")

    doLast {
        outputDir.mkdirs()

        logger.info("Writing creek-system-test-executor version: $creekVersion to $versionFile")
        versionFile.writeText(creekVersion)
    }
}

tasks.processResources {dependsOn(":writeVersionFile")}

defaultTasks("format", "static", "check")
