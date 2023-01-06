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

import org.creekservice.api.system.test.gradle.plugin.debug.PrepareDebug;

plugins {
    id("org.creekservice.system.test")
}

creek.systemTest {
    extraArguments("--echo-only")

    debugging {
        attachMePort.set(1234)
        baseServicePort.set(4321)
        serviceNames.set(setOf("service-a", "service-b"))
    }
}

creek.systemTest.debugging.serviceInstanceNames.set(setOf("instance-c", "instance-d"))

tasks.named<PrepareDebug>("systemTestPrepareDebug") {
    attachMeDirectory.set(layout.projectDirectory.dir("attachMe"))
}

tasks.named<PrepareDebug>("systemTestPrepareDebug") {
    attachMeDirectory.set(layout.projectDirectory.dir("attachMe"))
}