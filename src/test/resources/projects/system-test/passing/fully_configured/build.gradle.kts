/*
 * Copyright 2022 Creek Contributors (https://github.com/creek-service)
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
    id("org.creek.system.test")
}

repositories {
    mavenLocal() // Todo:
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/creek-service/*")
        credentials {
            username = "Creek-Bot-Token"
            password = "\u0067hp_LtyvXrQZen3WlKenUhv21Mg6NG38jn0AO2YH"
        }
    }
}

systemTest {
    additionalExecutorArguments.set(listOf("--echo-only"))

    testDirectory.set(file("$projectDir/custom-test"))
    resultDirectory.set(file("$buildDir/custom-result"))
    verificationTimeoutSeconds.set("90");
    suitePathPattern.set(".*include.*")
}