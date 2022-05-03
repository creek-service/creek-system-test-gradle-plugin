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

package org.creekservice.api.system.test.gradle.plugin.task;

import static org.creek.api.test.util.coverage.CodeCoverage.codeCoverageCmdLineArg;
import static org.creekservice.api.system.test.gradle.plugin.ExecutorVersion.defaultExecutorVersion;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.creek.api.test.util.TestPaths;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

@SuppressWarnings("ConstantConditions")
class SystemTestTaskTest {

    private static final Path PROJECT_DIR = TestPaths.projectRoot("src");
    private static final Path BUILD_DIR = PROJECT_DIR.resolve("build").toAbsolutePath();
    private static final Path TEST_DIR =
            PROJECT_DIR.resolve("src/test/resources/projects/functional").toAbsolutePath();

    private static final String TASK_NAME = ":systemTest";
    private static final String INIT_SCRIPT = "--init-script=" + TEST_DIR.resolve("init.gradle");

    @TempDir private Path projectDir;

    @BeforeEach
    void setUp() throws Exception {
        projectDir = projectDir.toRealPath();
        writeGradleProperties();
    }

    @CartesianTest
    void shouldSkipIfTestDirectoryDoesNotExist(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/empty");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(NO_SOURCE));
    }

    @CartesianTest
    void shouldSkipIfTestDirectoryEmpty(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/empty");
        givenDefaultSystemTestDirectory();

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(NO_SOURCE));
    }

    @CartesianTest
    void shouldExecuteWithDefaults(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString("--test-directory=" + projectDir.resolve("src/system-test")));
        assertThat(
                result.getOutput(),
                containsString(
                        "--result-directory="
                                + projectDir.resolve("build/test-results/system-test")));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=60"));
        assertThat(result.getOutput(), containsString("--include-suites=.*"));
    }

    @CartesianTest
    void shouldExecuteWithDefaultVersion(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString("SystemTestExecutor: " + defaultExecutorVersion()));
    }

    @CartesianTest
    void shouldExecuteWithSpecificVersion(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/specific_version");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("SystemTestExecutor: 0.1.13"));
    }

    @CartesianTest
    void shouldExecuteWithCustomProperties(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/fully_configured");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString("--test-directory=" + projectDir.resolve("custom-test")));
        assertThat(
                result.getOutput(),
                containsString("--result-directory=" + projectDir.resolve("build/custom-result")));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=120"));
        assertThat(result.getOutput(), containsString("--include-suites=.*include.*"));
    }

    @CartesianTest
    void shouldFailIfSystemTestConfigurationDoesNotContainExecutor(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/missing_executor_dep");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.FAIL, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(FAILED));
        assertThat(
                result.getOutput(),
                containsString(
                        "No system test executor dependency found in systemTest configuration."));
    }

    @CartesianTest
    void shouldFailOnBadConfig(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/invalid_config");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.FAIL, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(FAILED));
        assertThat(result.getOutput(), containsString("Invalid value for option"));
    }

    @CartesianTest
    void shouldExecuteWithOptions(
            @Values(strings = {"kotlin", "groovy"}) final String flavour,
            @Values(strings = {"6.4", "6.9.2", "7.0", "7.3", "7.4.2"}) final String gradleVersion) {
        // Given:
        givenProject(flavour + "/empty");
        givenTestSuite();

        // When:
        final BuildResult result =
                executeTask(
                        ExpectedOutcome.PASS,
                        gradleVersion,
                        "--extra-argument=--echo-only",
                        "--verification-timeout-seconds=76",
                        "--include-suites=.*cli.*");

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=76"));
        assertThat(result.getOutput(), containsString("--include-suites=.*cli.*"));
    }

    private void givenProject(final String projectPath) {
        TestPaths.copy(TEST_DIR.resolve(projectPath), projectDir);
    }

    private void givenDefaultSystemTestDirectory() {
        TestPaths.ensureDirectories(projectDir.resolve("src/system-test"));
    }

    private void givenTestSuite() {
        givenDefaultSystemTestDirectory();
        TestPaths.write(projectDir.resolve("src/system-test/test-suite.yml"), "");
    }

    private enum ExpectedOutcome {
        PASS,
        FAIL
    }

    private BuildResult executeTask(
            final ExpectedOutcome expectedOutcome,
            final String gradleVersion,
            final String... additionalArgs) {
        final List<String> args = new ArrayList<>(List.of(INIT_SCRIPT, "--stacktrace", TASK_NAME));
        args.addAll(List.of(additionalArgs));

        final GradleRunner runner =
                GradleRunner.create()
                        .withProjectDir(projectDir.toFile())
                        .withArguments(args)
                        .withPluginClasspath()
                        .withGradleVersion(gradleVersion);

        final BuildResult result =
                expectedOutcome == ExpectedOutcome.FAIL ? runner.buildAndFail() : runner.build();

        assertThat(result.task(TASK_NAME), is(notNullValue()));
        return result;
    }

    private void writeGradleProperties() {
        final List<String> options = new ArrayList<>(3);
        codeCoverageCmdLineArg(BUILD_DIR).ifPresent(options::add);

        if (!options.isEmpty()) {
            TestPaths.write(
                    projectDir.resolve("gradle.properties"),
                    "org.gradle.jvmargs=" + String.join(" ", options));
        }
    }
}
