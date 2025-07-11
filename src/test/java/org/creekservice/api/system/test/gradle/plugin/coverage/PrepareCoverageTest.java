/*
 * Copyright 2022-2025 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.api.system.test.gradle.plugin.coverage;

import static org.creekservice.api.system.test.gradle.plugin.TaskTestBase.ExpectedOutcome.PASS;
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.creekservice.api.system.test.gradle.plugin.TaskTestBase;
import org.creekservice.api.test.util.TestPaths;
import org.gradle.testkit.runner.BuildResult;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.MethodFactory;

@SuppressWarnings("ConstantConditions")
class PrepareCoverageTest extends TaskTestBase {

    // Change this to true locally to debug using attach-me plugin:
    private static final boolean DEBUG = false;

    private static final String TASK_NAME = ":systemTestPrepareCoverage";

    PrepareCoverageTest() {
        super(DEBUG);
    }

    @CartesianTest
    @MethodFactory("flavoursAndVersions")
    void shouldSkipPrepareCoverageIfNoJaCoCo(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SKIPPED));
    }

    @CartesianTest
    @MethodFactory("flavoursAndVersions")
    void shouldPrepareCoverage(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/with_jacoco");
        final Path agentJar = projectPath("build/creek/mounts/jacoco/jacocoagent.jar");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat("agent jar exists", Files.isRegularFile(agentJar));
    }

    @CartesianTest
    @MethodFactory("flavoursAndVersions")
    void shouldHandleAgentAlreadyExisting(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/with_jacoco");
        final Path agentJar = projectPath("build/creek/mounts/jacoco/jacocoagent.jar");
        TestPaths.write(agentJar, "existing");

        // When:
        final BuildResult result = executeTask(PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat("agent jar exists", Files.isRegularFile(agentJar));
        assertThat(
                TestPaths.readBytes(agentJar).length,
                is(not("existing".getBytes(StandardCharsets.UTF_8).length)));
    }

    private BuildResult executeTask(
            final ExpectedOutcome expectedOutcome,
            final String gradleVersion,
            final String... additionalArgs) {
        return executeTask(TASK_NAME, expectedOutcome, gradleVersion, additionalArgs);
    }
}
