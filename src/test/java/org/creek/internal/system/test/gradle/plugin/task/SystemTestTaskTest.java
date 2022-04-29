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

package org.creek.internal.system.test.gradle.plugin.task;

import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.file.Path;
import org.creek.api.test.util.TestPaths;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SystemTestTaskTest {

    public static final Path ROOT =
            TestPaths.projectRoot("src")
                    .resolve("src/test/resources/projects/system-test")
                    .toAbsolutePath();

    public static final String TASK_NAME = ":systemTest";

    @TempDir private Path projectDir;

    @Test
    void shouldSkipIfTestDirectoryDoesNotExist() {
        // Given:
        givenProject("passing/empty");

        // When:
        final BuildResult result = executeTask();

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(NO_SOURCE));
    }

    @Test
    void shouldSkipIfTestDirectoryEmpty() {
        // Given:
        givenProject("passing/empty");
        givenDirectory("src/system-test");

        // When:
        final BuildResult result = executeTask();

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(NO_SOURCE));
    }

    @Test
    void shouldExecuteWithDefaults() {
        // Given:
        givenProject("passing/default");

        // When:
        final BuildResult result = executeTask();

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString(
                        "--test-directory=/private" + projectDir.resolve("src/system-test")));
        assertThat(
                result.getOutput(),
                containsString(
                        "--result-directory=/private"
                                + projectDir.resolve("build/test-results/system-test")));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=60"));
        assertThat(result.getOutput(), containsString("--include-suites=.*"));
    }

    @Test
    void shouldExecuteWithSpecificVersion() {
        // Given:
        givenProject("passing/specific_version");

        // When:
        final BuildResult result = executeTask();

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("SystemTestExecutor: 0.1.10"));
    }

    @Test
    void shouldExecuteWithCustomProperties() {
        // Given:
        givenProject("passing/fully_configured");

        // When:
        final BuildResult result = executeTask();

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString("--test-directory=/private" + projectDir.resolve("custom-test")));
        assertThat(
                result.getOutput(),
                containsString(
                        "--result-directory=/private" + projectDir.resolve("build/custom-result")));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=90"));
        assertThat(result.getOutput(), containsString("--include-suites=.*include.*"));
    }

    // Todo:

    private void givenProject(final String projectPath) {
        TestPaths.copy(ROOT.resolve(projectPath), projectDir);
    }

    private void givenDirectory(final String path) {
        TestPaths.ensureDirectories(projectDir.resolve(path));
    }

    private BuildResult executeTask() {

        final BuildResult result =
                GradleRunner.create()
                        .withProjectDir(projectDir.toFile())
                        .withArguments(TASK_NAME)
                        // Todo: ~/ and optonal
                        //  .withEnvironment(
                        //  Map.of(
                        //    "JAVA_TOOL_OPTIONS",
                        //
                        // "-javaagent:/Users/andy/.attachme/attachme-agent-1.1.0.jar=port:7857,host:localhost
                        //
                        // -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000"))
                        .withPluginClasspath()
                        .build();

        assertThat(result.task(TASK_NAME), is(notNullValue()));

        return result;
    }
}

// Todo: tests for groovy syntax
// Todo: test different versions using GradleRunner.withGradleVersion
