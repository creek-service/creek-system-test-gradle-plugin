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

package org.creekservice.api.system.test.gradle.plugin;

import static org.creekservice.api.test.util.coverage.CodeCoverage.codeCoverageCmdLineArg;
import static org.creekservice.api.test.util.debug.RemoteDebug.remoteDebugArguments;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.creekservice.api.test.util.TestPaths;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;

public abstract class TaskTestBase {

    private static final Path PROJECT_DIR = TestPaths.projectRoot("src");
    private static final Path BUILD_DIR = PROJECT_DIR.resolve("build").toAbsolutePath();
    private static final Path TEST_DIR =
            PROJECT_DIR.resolve("src/test/resources/projects/functional").toAbsolutePath();

    private static final String INIT_SCRIPT = "--init-script=" + TEST_DIR.resolve("init.gradle");

    public enum ExpectedOutcome {
        PASS,
        FAIL
    }

    @TempDir private Path projectDir;
    private final boolean debug;

    protected TaskTestBase(final boolean debug) {
        this.debug = debug;
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        projectDir = projectDir.toRealPath();
        writeGradleProperties();
    }

    @Test
    void shouldNotCheckInWithDebuggingEnabled() {
        assertThat("Do not check in with debugging enabled", !debug);
    }

    protected Path projectPath(final String relativePath) {
        return projectDir.resolve(relativePath);
    }

    protected void givenProject(final String projectPath) {
        TestPaths.copy(TEST_DIR.resolve(projectPath), projectDir);
    }

    protected Path givenDirectory(final String relativePath) {
        final Path path = projectDir.resolve(relativePath);
        TestPaths.ensureDirectories(path);
        return path;
    }

    protected BuildResult executeTask(
            final String taskName,
            final ExpectedOutcome expectedOutcome,
            final String gradleVersion,
            final String... additionalArgs) {
        final List<String> args = new ArrayList<>(List.of(INIT_SCRIPT, "--stacktrace", taskName));
        args.addAll(List.of(additionalArgs));

        final GradleRunner runner =
                GradleRunner.create()
                        .withProjectDir(projectDir.toFile())
                        .withArguments(args)
                        .withPluginClasspath()
                        .withGradleVersion(gradleVersion);

        final BuildResult result =
                expectedOutcome == ExpectedOutcome.FAIL ? runner.buildAndFail() : runner.build();

        assertThat(result.getOutput(), result.task(taskName), is(notNullValue()));
        return result;
    }

    private void writeGradleProperties() {
        final List<String> options = new ArrayList<>(3);
        codeCoverageCmdLineArg(BUILD_DIR).ifPresent(options::add);

        if (debug) {
            options.addAll(remoteDebugArguments());
        }

        if (!options.isEmpty()) {
            TestPaths.write(
                    projectDir.resolve("gradle.properties"),
                    "org.gradle.jvmargs=" + String.join(" ", options));
        }
    }

    @SuppressWarnings("unused") // Invoked by reflection
    protected static ArgumentSets flavoursAndVersions() {
        final Collection<?> flavours = List.of("kotlin", "groovy");
        final Collection<?> gradleVersions = List.of("6.4"); // , "6.9.2", "7.0", "7.4.2");
        return ArgumentSets.argumentsForFirstParameter(flavours)
                .argumentsForNextParameter(gradleVersions);
    }
}
