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

package org.creekservice.api.system.test.gradle.plugin.test;

import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.CONTAINER_MOUNT_DIR;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.EXECUTOR_DEP_ARTEFACT_NAME;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.EXECUTOR_DEP_GROUP_NAME;
import static org.creekservice.api.system.test.gradle.plugin.coverage.SystemTestCoverageExtension.COVERAGE_EXT_NAME;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin;
import org.creekservice.api.system.test.gradle.plugin.coverage.SystemTestCoverageExtension;
import org.creekservice.api.system.test.gradle.plugin.debug.PrepareDebug;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;

/** Task for running Creek system tests. */
public abstract class SystemTest extends DefaultTask {

    /**
     * The path with in the container where a directory containing the AttachMe agent will be
     * mounted
     */
    public static final String CONTAINER_DEBUG_MOUNT = CONTAINER_MOUNT_DIR + "debug/";

    private final ConfigurableFileCollection classPath;
    private final PrepareDebug debugPrepareTask;

    /** Constructor. */
    public SystemTest() {
        this.classPath = getProject().getObjects().fileCollection();
        this.classPath.from((Callable<Object>) this::getSystemTestExecutor);
        this.classPath.from((Callable<Object>) this::getSystemTestExtensions);
        this.classPath.from((Callable<Object>) this::getSystemTestComponents);
        this.debugPrepareTask = prepareDebugTask(getProject());

        setDescription("Task for running Creek system tests");

        dependsOn(debugPrepareTask);

        initialiseCoverage();
    }

    /**
     * @return the source directory containing test
     */
    @SkipWhenEmpty
    @InputDirectory
    public abstract DirectoryProperty getTestDirectory();

    /**
     * @return the directory result files will be written to.
     */
    @OutputDirectory
    public abstract DirectoryProperty getResultDirectory();

    /**
     * @return dependencies of the system test executor.
     */
    @Internal
    public abstract ConfigurableFileCollection getSystemTestExecutor();

    /**
     * @return dependencies of the system test extensions.
     */
    @Internal
    public abstract ConfigurableFileCollection getSystemTestExtensions();

    /**
     * @return dependencies of the components being system tested.
     */
    @Internal
    public abstract ConfigurableFileCollection getSystemTestComponents();

    /**
     * @return verification timeout property.
     */
    @Option(
            option = "verification-timeout-seconds",
            description =
                    "Set an optional custom verifier timeout. The verifier timeout is the maximum"
                        + " amount of time the system tests will wait for a defined expectation to"
                        + " be met. A longer timeout will mean tests have more time for"
                        + " expectations to be met, but may run slower as a consequence.")
    @Input
    public abstract Property<String> getVerificationTimeoutSeconds();

    /**
     * @return suite path pattern property.
     */
    @Option(
            option = "include-suites",
            description =
                    "Set an optional regular expression pattern to limit the test suites to run."
                        + " Only test suites whose relative path matches the supplied pattern will"
                        + " be included.")
    @Input
    public abstract Property<String> getSuitesPathPattern();

    /**
     * @return additional command line arguments to pass to the executor
     */
    @Input
    public abstract ListProperty<String> getExtraArguments();

    /**
     * Method to allow setting extra arguments from the command line.
     *
     * @param args the extra arguments.
     */
    @SuppressWarnings("unused") // Invoked via reflection
    @Option(
            option = "extra-argument",
            description = "Any additional arguments to use when running system tests.")
    public void setExtraArgumentsFromOption(final List<String> args) {
        getExtraArguments().set(args);
    }

    /**
     * The port on which the attachMe plugin is listening on.
     *
     * <p>This is the port the attachMe agent running within the microservice's process will call
     * out on to ask the debugger to attach.
     *
     * @return the port the attachMe plugin is listening on.
     */
    @Input
    public abstract Property<Integer> getDebugAttachMePort();

    /**
     * The base debug port.
     *
     * <p>The port the first service being debugged will listen on for the debugger to attach.
     * Subsequent services being debugged will use sequential port numbers.
     *
     * @return the base port number used for debugging.
     */
    @Input
    public abstract Property<Integer> getDebugBaseServicePort();

    /**
     * The set of services to be debugged.
     *
     * @return set of services to debug.
     */
    @Input
    public abstract SetProperty<String> getDebugServiceNames();

    /**
     * Method to allow setting debug service names the command line.
     *
     * @param args the service names
     */
    @SuppressWarnings("unused") // Invoked via reflection
    @Option(option = "debug-service", description = "The name(s) of service(s) to debug")
    public void setDebugServices(final List<String> args) {
        getDebugServiceNames().set(Set.copyOf(args));
    }

    /**
     * The set of service instances to be debugged.
     *
     * <p>An instance name is the name of the service with a dash and the instance number appended,
     * e.g. {@code my-service-1}.
     *
     * @return set of service instances to debug.
     */
    @Input
    public abstract SetProperty<String> getDebugServiceInstanceNames();

    /**
     * Method to allow setting debug service instance names the command line.
     *
     * @param args the instance names
     */
    @SuppressWarnings("unused") // Invoked via reflection
    @Option(
            option = "debug-service-instance",
            description =
                    "The name(s) of service instances(s) to debug: <service-name>:<instance-num>")
    public void setDebugServiceInstances(final List<String> args) {
        getDebugServiceInstanceNames().set(Set.copyOf(args));
    }

    /** Run the task. */
    @TaskAction
    public void run() {
        cleanUp();
        checkDependenciesIncludesRunner();

        getProject()
                .javaexec(
                        spec -> {
                            spec.getMainClass()
                                    .set(
                                            "org.creekservice.api.system.test.executor.SystemTestExecutor");
                            spec.setClasspath(classPath);
                            spec.setArgs(arguments());
                            spec.jvmArgs(jvmArgs());
                        });
    }

    private void initialiseCoverage() {
        final Action<Object> initializer =
                ignored -> {
                    dependsOn(
                            getProject()
                                    .getTasksByName(
                                            SystemTestPlugin.PREPARE_COVERAGE_TASK_NAME, false));

                    getExtensions()
                            .create(COVERAGE_EXT_NAME, SystemTestCoverageExtension.class, this);
                };

        // Initialize coverage if the Jacoco plugin is present, or added later:
        getProject()
                .getPlugins()
                .matching(p -> p.getClass().equals(JacocoPlugin.class))
                .all(initializer);
    }

    private void cleanUp() {
        final SystemTestCoverageExtension ext =
                getExtensions().findByType(SystemTestCoverageExtension.class);
        if (ext != null) {
            ext.cleanUp();
            getLogger().info("Coverage data will be written to " + ext.getDestinationFile());
        }
    }

    private void checkDependenciesIncludesRunner() {
        final Configuration configuration =
                getProject()
                        .getConfigurations()
                        .getByName(SystemTestPlugin.EXECUTOR_CONFIGURATION_NAME);
        configuration.resolve();

        final Optional<Dependency> executorDep =
                configuration.getDependencies().stream()
                        .filter(dep -> EXECUTOR_DEP_GROUP_NAME.equals(dep.getGroup()))
                        .filter(dep -> EXECUTOR_DEP_ARTEFACT_NAME.equals(dep.getName()))
                        .findFirst();

        if (executorDep.isEmpty()) {
            throw new MissingExecutorDependencyException();
        }

        getLogger().debug("Using system test executor version: " + executorDep.get().getVersion());
    }

    private List<String> arguments() {
        final List<String> arguments = new ArrayList<>();
        arguments.addAll(commonArguments());
        arguments.addAll(debugArguments());
        arguments.addAll(coverageArguments());
        arguments.addAll(javaToolOptionsArgument());
        arguments.addAll(getExtraArguments().get());
        return arguments;
    }

    private List<String> commonArguments() {
        return List.of(
                "--test-directory="
                        + getTestDirectory().getAsFile().get().toPath().toAbsolutePath(),
                "--result-directory="
                        + getResultDirectory().getAsFile().get().toPath().toAbsolutePath(),
                "--verifier-timeout-seconds=" + getVerificationTimeoutSeconds().getOrNull(),
                "--include-suites=" + getSuitesPathPattern().getOrNull());
    }

    private List<String> debugArguments() {
        if (nothingToDebug()) {
            return List.of();
        }

        final List<String> args = new ArrayList<>();
        args.add("--debug-service-port=" + getDebugBaseServicePort().get());
        args.add(
                "--mount-read-only="
                        + debugPrepareTask.getMountDirectory().get()
                        + "="
                        + CONTAINER_DEBUG_MOUNT);

        final String services = String.join(",", getDebugServiceNames().get());
        if (!services.isBlank()) {
            args.add("--debug-service=" + services);
        }

        final String instances = String.join(",", getDebugServiceInstanceNames().get());
        if (!instances.isBlank()) {
            args.add("--debug-service-instance=" + instances);
        }

        return args;
    }

    private List<String> coverageArguments() {
        final SystemTestCoverageExtension ext =
                getExtensions().findByType(SystemTestCoverageExtension.class);
        if (ext == null) {
            return List.of();
        }

        return ext.mountOptions();
    }

    private List<String> javaToolOptionsArgument() {
        final List<String> options = new ArrayList<>(2);
        options.add(debugJavaToolOptions());
        options.add(coverageJavaToolOptions());
        options.removeIf(String::isEmpty);
        if (options.isEmpty()) {
            return List.of();
        }

        return List.of("--env=JAVA_TOOL_OPTIONS=\"" + String.join(" ", options) + "\"");
    }

    private String debugJavaToolOptions() {
        if (nothingToDebug()) {
            return "";
        }

        final Path agentJar =
                debugPrepareTask
                        .getAgentJarFileName()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No AttachMe agent jar found."
                                                        + System.lineSeparator()
                                                        + "Debugging services requires the AttachMe"
                                                        + " IntelliJ plugin to have been installed"
                                                        + " and running."
                                                        + System.lineSeparator()
                                                        + "See: https://github.com/creek-service/"
                                                        + "creek-system-test-gradle-plugin#debugging-system-tests"));

        return "-javaagent:"
                + CONTAINER_DEBUG_MOUNT
                + agentJar
                + "=host:host.docker.internal,port:"
                + getDebugAttachMePort().get()
                + " -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:${SERVICE_DEBUG_PORT}";
    }

    private String coverageJavaToolOptions() {
        final SystemTestCoverageExtension ext =
                getExtensions().findByType(SystemTestCoverageExtension.class);
        if (ext == null) {
            return "";
        }

        return ext.asJavaToolOptions();
    }

    private boolean nothingToDebug() {
        return getDebugServiceNames().get().isEmpty()
                && getDebugServiceInstanceNames().get().isEmpty();
    }

    private List<String> jvmArgs() {
        final Object jvmArgs = getProject().findProperty("org.gradle.jvmargs");
        if ((!(jvmArgs instanceof String))) {
            return List.of();
        }

        return List.of(((String) jvmArgs).split("\\s+"));
    }

    private static PrepareDebug prepareDebugTask(final Project project) {
        return (PrepareDebug)
                project.getTasksByName(SystemTestPlugin.PREPARE_DEBUG_TASK_NAME, false)
                        .iterator()
                        .next();
    }

    private static final class MissingExecutorDependencyException extends GradleException {

        MissingExecutorDependencyException() {
            super(
                    "No system test executor dependency found in "
                            + SystemTestPlugin.EXECUTOR_CONFIGURATION_NAME
                            + " configuration. Please ensure the configuration contains "
                            + EXECUTOR_DEP_GROUP_NAME
                            + ":"
                            + EXECUTOR_DEP_ARTEFACT_NAME);
        }
    }
}
