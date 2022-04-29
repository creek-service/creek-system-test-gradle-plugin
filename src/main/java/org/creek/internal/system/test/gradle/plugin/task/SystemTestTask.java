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


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.gradle.api.DefaultTask;
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
    @Internal // Todo: add justicificatioin, what if these change? Will it rebuild?
    public abstract ConfigurableFileCollection getSystemTestDeps();

    // Todo: document
    @Option(
            option = "verification-timeout-seconds",
            description =
                    "Set an optional custom verifier timeout. "
                            + "The verifier timeout is the maximum amount of time the system tests "
                            + "will wait for a defined expectation to be met. A longer timeout will mean "
                            + "tests have more time for expectations to be met, but may run slower as a consequence.")
    @Input
    public abstract Property<String> getVerificationTimeoutSeconds();

    // Todo: document
    @Option(
            option = "include-suites",
            description =
                    "Set an optional regular expression pattern to limit the test suites to run. "
                            + "Only test suites whose relative path matches the supplied pattern will be included.")
    @Input
    public abstract Property<String> getSuitesPathPattern();

    // Todo: make option
    /** @return additional command line arguments to pass to the executor */
    @Input
    public abstract ListProperty<String> getAdditionalExecutorArguments();

    @TaskAction
    public void run() {
        // Todo: Can we invoke as module?

        getProject()
                .javaexec(
                        spec -> {
                            spec.getMainClass()
                                    .set("org.creek.api.system.test.executor.SystemTestExecutor");
                            spec.setClasspath(classPath);
                            spec.setArgs(arguments());
                        });
    }

    private List<String> arguments() {
        final List<String> arguments = new ArrayList<>();
        arguments.add(
                "--test-directory="
                        + getTestDirectory().getAsFile().get().toPath().toAbsolutePath());
        arguments.add(
                "--result-directory="
                        + getResultDirectory().getAsFile().get().toPath().toAbsolutePath());

        final String verifyTimeout = getVerificationTimeoutSeconds().getOrNull();
        if (verifyTimeout != null) {
            arguments.add("--verifier-timeout-seconds=" + verifyTimeout);
        }

        // Todo: Adjust logging level dynamically? Use `getLogger().isInfoEnabled()`...

        arguments.addAll(getAdditionalExecutorArguments().get());
        return arguments;
    }
}

// Todo: test
