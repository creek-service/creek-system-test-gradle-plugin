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

package org.creekservice.api.system.test.gradle.plugin.debug;

import static org.creekservice.api.system.test.gradle.plugin.debug.AttachMeAgentJarFinder.findAttacheMeAgentJar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachMeAgentJarFinderTest {

    @TempDir private Path testDir;

    @Test
    void shouldReturnEmptyIfNoAttachMeDir() {
        assertThat(findAttacheMeAgentJar(testDir), is(Optional.empty()));
    }

    @Test
    void shouldReturnEmptyIfNoAgentJar() throws Exception {
        // Given:
        Files.createDirectories(testDir);

        // Then:
        assertThat(findAttacheMeAgentJar(testDir), is(Optional.empty()));
    }

    @Test
    void shouldPickLatestAgentJar() throws Exception {
        // Given:
        Files.createDirectories(testDir);
        Files.createFile(testDir.resolve("aaa.jar"));
        Files.createFile(testDir.resolve("attachme-agent-1.0.0.jar"));
        Files.createFile(testDir.resolve("attachme-agent-1.1.0.jar"));
        Files.createFile(testDir.resolve("bbb.jar"));

        // When:
        final Optional<Path> result = findAttacheMeAgentJar(testDir);

        // Then:
        assertThat(result, is(Optional.of(testDir.resolve("attachme-agent-1.1.0.jar"))));
    }
}
