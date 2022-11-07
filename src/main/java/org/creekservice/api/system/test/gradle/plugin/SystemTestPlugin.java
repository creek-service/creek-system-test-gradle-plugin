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

import static org.creekservice.api.system.test.gradle.plugin.ExecutorVersion.defaultExecutorVersion;

import java.time.Duration;
import java.util.List;
import org.creekservice.api.system.test.gradle.plugin.task.SystemTest;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testing.base.plugins.TestingBasePlugin;

/** Plugin for running Creek system tests. */
public final class SystemTestPlugin implements Plugin<Project> {

    /** Creek extension name */
    public static final String CREEK_EXTENSION_NAME = "creek";

    /** System test extension name, i.e. `creek.systemTest` */
    public static final String TEST_EXTENSION_NAME = "systemTest";

    /** Name of dependency configuration for storing the system test executor. */
    public static final String EXECUTOR_CONFIGURATION_NAME = "systemTestExecutor";

    /** Name of dependency configuration for storing system test extensions. */
    public static final String EXTENSION_CONFIGURATION_NAME = "systemTestExtension";

    /**
     * Name of dependency configuration for storing dependencies containing aggregate and service
     * descriptors.
     */
    public static final String COMPONENT_CONFIGURATION_NAME = "systemTestComponent";

    /** Name of the system test task */
    public static final String SYSTEM_TEST_TASK_NAME = "systemTest";

    /** Standard Creek group name. */
    public static final String GROUP_NAME = "creek";

    /** The default directory in which to load system tests packages from. */
    public static final String DEFAULT_TESTS_DIR_NAME = "src/system-test";
    /** The default directory to which test results are written. */
    public static final String DEFAULT_RESULTS_DIR_NAME =
            TestingBasePlugin.TEST_RESULTS_DIR_NAME + "/system-test";
    /** The default expectation timeout. */
    public static final Duration DEFAULT_EXPECTATION_TIMEOUT = Duration.ofMinutes(1);
    /** The default suite include pattern: all suites. */
    public static final String DEFAULT_SUITES_PATTERN = ".*";

    /** Artifact group for executor */
    public static final String EXECUTOR_DEP_GROUP_NAME = "org.creekservice";

    /** Artifact name for executor */
    public static final String EXECUTOR_DEP_ARTEFACT_NAME = "creek-system-test-executor";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(BasePlugin.class);

        final SystemTestExtension extension = registerExtension(project);
        registerSystemTestTask(project, extension);
        registerSystemTestExecutorConfiguration(project);
        registerSystemTestExtensionConfiguration(project);
        registerSystemTestComponentConfiguration(project);
    }

    private SystemTestExtension registerExtension(final Project project) {
        final ExtensionAware creekExt = ensureCreekExtension(project);
        final SystemTestExtension extension =
                creekExt.getExtensions().create(TEST_EXTENSION_NAME, SystemTestExtension.class);

        extension
                .getTestDirectory()
                .convention(project.getLayout().getProjectDirectory().dir(DEFAULT_TESTS_DIR_NAME));
        extension
                .getResultDirectory()
                .convention(project.getLayout().getBuildDirectory().dir(DEFAULT_RESULTS_DIR_NAME));
        extension
                .getVerificationTimeoutSeconds()
                .convention(String.valueOf(DEFAULT_EXPECTATION_TIMEOUT.toSeconds()));
        extension.getSuitePathPattern().convention(DEFAULT_SUITES_PATTERN);
        extension.getExtraArguments().convention(List.of());
        return extension;
    }

    private void registerSystemTestTask(
            final Project project, final SystemTestExtension extension) {
        final SystemTest task = project.getTasks().create(SYSTEM_TEST_TASK_NAME, SystemTest.class);

        task.setGroup(GROUP_NAME);
        task.getTestDirectory().set(extension.getTestDirectory());
        task.getResultDirectory().set(extension.getResultDirectory());
        task.getExtraArguments().set(extension.getExtraArguments());
        task.getVerificationTimeoutSeconds().set(extension.getVerificationTimeoutSeconds());
        task.getSuitesPathPattern().set(extension.getSuitePathPattern());

        task.getDebugAttachMePort().set(extension.getDebugging().getAttachMePort());
        task.getDebugBaseServicePort().set(extension.getDebugging().getBaseServicePort());
        task.getDebugServiceNames().set(extension.getDebugging().getServiceNames());
        task.getDebugServiceInstanceNames().set(extension.getDebugging().getServiceInstanceNames());

        project.getTasksByName(LifecycleBasePlugin.CHECK_TASK_NAME, false)
                .forEach(checkTask -> checkTask.dependsOn(task));
    }

    private void registerSystemTestExecutorConfiguration(final Project project) {
        final Configuration cfg = project.getConfigurations().create(EXECUTOR_CONFIGURATION_NAME);
        cfg.setVisible(false);
        cfg.setTransitive(true);
        cfg.setCanBeConsumed(false);
        cfg.setCanBeResolved(true);
        cfg.setDescription("Dependency for the Creek system test executor");

        final String pluginDep =
                EXECUTOR_DEP_GROUP_NAME
                        + ":"
                        + EXECUTOR_DEP_ARTEFACT_NAME
                        + ":"
                        + defaultExecutorVersion();
        final DependencyHandler projectDeps = project.getDependencies();
        cfg.defaultDependencies(deps -> deps.add(projectDeps.create(pluginDep)));

        project.getTasks()
                .withType(SystemTest.class)
                .configureEach(task -> task.getSystemTestExecutor().from(cfg));
    }

    private void registerSystemTestExtensionConfiguration(final Project project) {
        final Configuration cfg = project.getConfigurations().create(EXTENSION_CONFIGURATION_NAME);
        cfg.setVisible(false);
        cfg.setTransitive(true);
        cfg.setCanBeConsumed(false);
        cfg.setCanBeResolved(true);
        cfg.setDescription("Creek system test extensions");

        project.getTasks()
                .withType(SystemTest.class)
                .configureEach(task -> task.getSystemTestExtensions().from(cfg));
    }

    private void registerSystemTestComponentConfiguration(final Project project) {
        final Configuration cfg = project.getConfigurations().create(COMPONENT_CONFIGURATION_NAME);
        cfg.setVisible(false);
        cfg.setTransitive(true);
        cfg.setCanBeConsumed(false);
        cfg.setCanBeResolved(true);
        cfg.setDescription(
                "Creek components: the services under test and any aggregates they interact with");

        project.getTasks()
                .withType(SystemTest.class)
                .configureEach(task -> task.getSystemTestComponents().from(cfg));
    }

    private <T extends ExtensionAware> ExtensionAware ensureCreekExtension(final Project project) {
        final ExtensionContainer extensions = project.getExtensions();
        final Object maybeExt = extensions.findByName(SystemTestPlugin.CREEK_EXTENSION_NAME);
        if (maybeExt != null) {
            return (ExtensionAware) maybeExt;
        }

        return extensions.create(SystemTestPlugin.CREEK_EXTENSION_NAME, CreekSpec.class);
    }

    /** The `creek` extension namespace */
    public abstract static class CreekSpec implements ExtensionAware {

        @Override
        public abstract ExtensionContainer getExtensions();
    }
}
