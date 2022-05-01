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


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/** Task for running Creek system tests. */
public abstract class SystemTestTask extends DefaultTask {

    private final ConfigurableFileCollection classPath;

    public SystemTestTask() {
        this.classPath = getProject().getObjects().fileCollection();
        classPath.from((Callable<Object>) this::getSystemTestDeps);

        setDescription("Runs system tests");
    }

    /** @return the source directory containing test */
    @SkipWhenEmpty
    @InputDirectory
    public abstract DirectoryProperty getTestDirectory();

    /** @return the directory result files will be written to. */
    @OutputDirectory
    public abstract DirectoryProperty getResultDirectory();

    /** @return dependencies of the system test runner. */
    @Internal
    public abstract ConfigurableFileCollection getSystemTestDeps();

    @Option(
            option = "verification-timeout-seconds",
            description =
                    "Set an optional custom verifier timeout. "
                            + "The verifier timeout is the maximum amount of time the system tests "
                            + "will wait for a defined expectation to be met. A longer timeout will mean "
                            + "tests have more time for expectations to be met, but may run slower as a consequence.")
    @Input
    public abstract Property<String> getVerificationTimeoutSeconds();

    @Option(
            option = "include-suites",
            description =
                    "Set an optional regular expression pattern to limit the test suites to run. "
                            + "Only test suites whose relative path matches the supplied pattern will be included.")
    @Input
    public abstract Property<String> getSuitesPathPattern();

    /** @return additional command line arguments to pass to the executor */
    @Input
    public abstract ListProperty<String> getExtraArguments();

    /** Method to allow setting extra arguments from the command line. */
    @Option(
            option = "extra-argument",
            description = "Any additional arguments to use when running system tests.")
    public void setExtraArgumentsFromOption(final List<String> args) {
        getExtraArguments().set(args);
    }

    @TaskAction
    public void run() {
        checkDependenciesIncludesRunner();

        getProject()
                .javaexec(
                        spec -> {
                            spec.getMainClass()
                                    .set("org.creek.api.system.test.executor.SystemTestExecutor");
                            spec.setClasspath(classPath);
                            spec.setArgs(arguments());
                        });
    }

    private void checkDependenciesIncludesRunner() {
        final Configuration configuration =
                getProject().getConfigurations().getByName(SystemTestPlugin.CONFIGURATION_NAME);
        configuration.resolve();

        final Optional<Dependency> executorDep =
                configuration.getDependencies().stream()
                        .filter(dep -> "org.creek".equals(dep.getGroup()))
                        .filter(dep -> "creek-system-test-executor".equals(dep.getName()))
                        .findFirst();

        if (executorDep.isEmpty()) {
            throw new MissingExecutorDependency();
        }

        getLogger().debug("Using system test executor version: " + executorDep.get().getVersion());
    }

    private List<String> arguments() {
        final List<String> arguments = new ArrayList<>();
        arguments.add(
                "--test-directory="
                        + getTestDirectory().getAsFile().get().toPath().toAbsolutePath());
        arguments.add(
                "--result-directory="
                        + getResultDirectory().getAsFile().get().toPath().toAbsolutePath());

        arguments.add("--verifier-timeout-seconds=" + getVerificationTimeoutSeconds().getOrNull());

        arguments.add("--include-suites=" + getSuitesPathPattern().getOrNull());

        arguments.addAll(getExtraArguments().get());
        return arguments;
    }

    private static final class MissingExecutorDependency extends RuntimeException {

        MissingExecutorDependency() {
            super(
                    "No system test executor dependency found in "
                            + SystemTestPlugin.CONFIGURATION_NAME
                            + " configuration.");
        }
    }
}
