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

package org.creekservice.api.system.test.gradle.plugin.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.creekservice.api.system.test.gradle.plugin.ExecutorVersion.defaultExecutorVersion;
import static org.creekservice.api.system.test.gradle.plugin.TaskTestBase.ExpectedOutcome.FAIL;
import static org.creekservice.api.system.test.gradle.plugin.TaskTestBase.ExpectedOutcome.PASS;
import static org.creekservice.api.test.hamcrest.PathMatchers.doesNotExist;
import static org.creekservice.api.test.util.TestPaths.delete;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.creekservice.api.system.test.gradle.plugin.TaskTestBase;
import org.creekservice.api.test.util.TestPaths;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Disabled;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.MethodFactory;

@SuppressWarnings("ConstantConditions")
class SystemTestTest extends TaskTestBase {

    // Change this to true locally to debug using attach-me plugin:
    private static final boolean DEBUG = false;

    private static final String TASK_NAME = ":systemTest";
    private static final String JACOCO_COVERAGE_AGENT =
            "-javaagent:/opt/creek/mounts/jacoco/jacocoagent.jar=destfile=/opt/creek/mounts/coverage/systemTest.exec"
                + ",append=true,inclnolocationclasses=false,dumponexit=true,output=file,jmx=false";

    SystemTestTest() {
        super(DEBUG);
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldSkipIfTestDirectoryDoesNotExist(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/empty");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(NO_SOURCE));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldSkipIfTestDirectoryEmpty(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/empty");
        givenDirectory("src/system-test");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(NO_SOURCE));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithDefaults(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString("--test-directory=" + projectPath("src/system-test")));
        assertThat(
                result.getOutput(),
                containsString(
                        "--result-directory=" + projectPath("build/test-results/system-test")));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=60"));
        assertThat(result.getOutput(), containsString("--include-suites=.*"));

        assertThat(
                result.getOutput(),
                containsString("SystemTestExecutor: " + defaultExecutorVersion()));

        assertThat(
                "class-path should not include Guava as this is used as a dummy extension and"
                        + " component by other tests",
                result.getOutput(),
                not(matchesPattern(Pattern.compile(".*--class-path=.*guava.*", Pattern.DOTALL))));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithExplicitComponents(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/explicit-component");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                matchesPattern(Pattern.compile(".*--class-path=.*guava.*", Pattern.DOTALL)));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithExplicitExtension(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/explicit-extension");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                matchesPattern(Pattern.compile(".*--class-path=.*guava.*", Pattern.DOTALL)));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithSpecificVersion(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/specific_version");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("SystemTestExecutor: 0.2.0-SNAPSHOT"));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithCustomProperties(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/fully_configured");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString("--test-directory=" + projectPath("custom-test")));
        assertThat(
                result.getOutput(),
                containsString("--result-directory=" + projectPath("build/custom-result")));
        assertThat(result.getOutput(), containsString("--verifier-timeout-seconds=120"));
        assertThat(result.getOutput(), containsString("--include-suites=.*include.*"));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldFailIfSystemTestConfigurationDoesNotContainExecutor(
            final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/missing_executor_dep");

        // When:
        final BuildResult result = executeTask(FAIL, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(FAILED));
        assertThat(
                result.getOutput(),
                containsString(
                        "No system test executor dependency found in systemTestExecutor"
                                + " configuration."));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldFailOnBadConfig(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/invalid_config");

        // When:
        final BuildResult result = executeTask(FAIL, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(FAILED));
        assertThat(result.getOutput(), containsString("Invalid value for option"));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithOptions(final String flavour, final String gradleVersion) {
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
        assertThat(result.getOutput(), containsString("--debug-service=<Not Set>"));
        assertThat(result.getOutput(), containsString("--debug-service-instance=<Not Set>"));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithDebugServices(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("--debug-service-port=4321"));
        assertThat(result.getOutput(), containsString("--debug-service=service-a,service-b"));
        assertThat(
                result.getOutput(),
                containsString("--debug-service-instance=instance-c,instance-d"));
        assertThat(
                result.getOutput(),
                containsString(
                        "--mount-read-only="
                                + projectPath("build/creek/mounts/debug")
                                + "=/opt/creek/mounts/debug"));
        assertThat(result.getOutput(), not(containsString("--env=JAVA_TOOL_OPTIONS=")));
        assertThat(
                result.getOutput(),
                containsString("--debug-env=JAVA_TOOL_OPTIONS=" + attachMeDebugAgent(1234)));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithDebugOptions(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug_options");

        // When:
        final BuildResult result =
                executeTask(
                        ExpectedOutcome.PASS,
                        gradleVersion,
                        "--extra-argument=--echo-only",
                        "--debug-service=service-a,service-b",
                        "--debug-service-instance=instance-c",
                        "--debug-service-instance=instance-d");

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("--debug-service=service-a,service-b"));
        assertThat(
                result.getOutput(),
                containsString("--debug-service-instance=instance-c,instance-d"));
        assertThat(
                result.getOutput(),
                containsString(
                        "--mount-read-only="
                                + projectPath("build/creek/mounts/debug")
                                + "=/opt/creek/mounts/debug"));
        assertThat(result.getOutput(), not(containsString("--env=JAVA_TOOL_OPTIONS=")));
        assertThat(
                result.getOutput(),
                containsString("--debug-env=JAVA_TOOL_OPTIONS=" + attachMeDebugAgent(7857)));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithDebugOptionsJustService(
            final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug_options");

        // When:
        final BuildResult result =
                executeTask(
                        ExpectedOutcome.PASS,
                        gradleVersion,
                        "--extra-argument=--echo-only",
                        "--debug-service=service-a,service-b");

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("--debug-service=service-a,service-b"));
        assertThat(result.getOutput(), containsString("--debug-service-instance=<Not Set>"));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithDebugOptionsJustInstance(
            final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug_options");

        // When:
        final BuildResult result =
                executeTask(
                        ExpectedOutcome.PASS,
                        gradleVersion,
                        "--extra-argument=--echo-only",
                        "--debug-service-instance=instance-c",
                        "--debug-service-instance=instance-d");

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("--debug-service=<Not Set>"));
        assertThat(
                result.getOutput(),
                containsString("--debug-service-instance=instance-c,instance-d"));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldFailWithDebugServicesIfNoAgentJar(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug");
        delete(projectPath("attachMe/attachme-agent-1.2.3.jar"));

        // When:
        final BuildResult result = executeTask(FAIL, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(FAILED));
        assertThat(result.getOutput(), containsString("No AttachMe agent jar found."));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldFailWithDebugServicesIfNoAttachMeDir(
            final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug");
        delete(projectPath("attachMe"));

        // When:
        final BuildResult result = executeTask(FAIL, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(FAILED));
        assertThat(result.getOutput(), containsString("No AttachMe agent jar found."));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldPrepareDebugBeforeSystemTest(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/debug");

        // When:
        final BuildResult result = executeTask(PASS, gradleVersion);

        // Then:
        assertThat(result.task(":systemTestPrepareDebug").getOutcome(), is(SUCCESS));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldRunSystemTestAsPartOfCheckTask(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");

        // When:
        final BuildResult result = executeTask(":check", ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(":check").getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("SystemTestExecutor: "));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldDeleteOutputDirectoryOnClean(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");
        final Path resultsDir = givenDirectory("build/test-results/system-test");

        // When:
        final BuildResult result =
                executeTask(":cleanSystemTest", ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(":cleanSystemTest").getOutcome(), is(SUCCESS));
        assertThat(Files.exists(resultsDir), is(false));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldPlayNicelyWithOthers(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/other_creek_plugin");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldNotPrepareCoverageFirstIfJaCoCoNotInstalled(
            final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/default");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.task(":systemTestPrepareCoverage"), is(nullValue()));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldPrepareCoverageFirstIfJaCoCoInstalled(
            final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/with_jacoco");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.task(":systemTestPrepareCoverage").getOutcome(), is(SUCCESS));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldDeleteAnyExistingCoverageOutputBeforeRunningSystemTest(
            final String flavour, final String gradleVersion) throws Exception {
        // Given:
        givenProject(flavour + "/with_jacoco");

        final Path resultFile =
                givenDirectory("build/creek/mounts/coverage").resolve("systemTest.exec");
        Files.write(resultFile, "Some Data".getBytes(UTF_8));

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(resultFile, doesNotExist());
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldExecuteWithCoverage(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/with_jacoco");

        // When:
        final BuildResult result = executeTask(ExpectedOutcome.PASS, gradleVersion);

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(
                result.getOutput(),
                containsString(
                        "--mount-read-only="
                                + projectPath("build/creek/mounts/jacoco")
                                + "=/opt/creek/mounts/jacoco"));
        assertThat(
                result.getOutput(),
                containsString(
                        "--mount-writable="
                                + projectPath("build/creek/mounts/coverage")
                                + "=/opt/creek/mounts/coverage"));
        assertThat(
                result.getOutput(),
                containsString("--env=JAVA_TOOL_OPTIONS=" + JACOCO_COVERAGE_AGENT));

        assertThat(result.getOutput(), not(containsString("--debug-env=JAVA_TOOL_OPTIONS=")));
    }

    @CartesianTest(name = "{displayName} flavour={0}, gradleVersion={1}")
    @MethodFactory("flavoursAndVersions")
    void shouldSupportDebuggingAndCoverage(final String flavour, final String gradleVersion) {
        // Given:
        givenProject(flavour + "/with_jacoco");

        // When:
        final BuildResult result =
                executeTask(
                        ExpectedOutcome.PASS,
                        gradleVersion,
                        "--extra-argument=--echo-only",
                        "--debug-service=service-a");

        // Then:
        assertThat(result.task(TASK_NAME).getOutcome(), is(SUCCESS));
        assertThat(result.getOutput(), containsString("--debug-service=service-a"));
        assertThat(
                result.getOutput(),
                containsString(
                        "--mount-read-only="
                                + projectPath("build/creek/mounts/jacoco")
                                + "=/opt/creek/mounts/jacoco,"
                                + projectPath("build/creek/mounts/debug")
                                + "=/opt/creek/mounts/debug"));
        assertThat(
                result.getOutput(),
                containsString(
                        "--mount-writable="
                                + projectPath("build/creek/mounts/coverage")
                                + "=/opt/creek/mounts/coverage"));
        assertThat(
                result.getOutput(),
                containsString("--env=JAVA_TOOL_OPTIONS=" + JACOCO_COVERAGE_AGENT));

        assertThat(
                result.getOutput(),
                containsString(
                        "--debug-env=JAVA_TOOL_OPTIONS="
                                + attachMeDebugAgent(7857)
                                + " "
                                + JACOCO_COVERAGE_AGENT));
    }

    private void givenTestSuite() {
        givenDirectory("src/system-test");
        TestPaths.write(projectPath("src/system-test/test-suite.yml"), "");
    }

    private BuildResult executeTask(
            final ExpectedOutcome expectedOutcome,
            final String gradleVersion,
            final String... additionalArgs) {
        return executeTask(TASK_NAME, expectedOutcome, gradleVersion, additionalArgs);
    }

    private static String attachMeDebugAgent(final int port) {
        return "-javaagent:/opt/creek/mounts/debug/attachme-agent-1.2.3.jar=host:host.docker.internal,port:"
                + port
                + " -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:${SERVICE_DEBUG_PORT}";
    }
}
