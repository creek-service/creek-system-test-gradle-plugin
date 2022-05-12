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
import org.creekservice.api.system.test.gradle.plugin.task.SystemTestTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testing.base.plugins.TestingBasePlugin;

/** Plugin for running Creek system tests. */
public final class SystemTestPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "systemTest";
    public static final String CONFIGURATION_NAME = "systemTestExecutor";
    public static final String SYSTEM_TEST_TASK_NAME = "systemTest";
    public static final String GROUP_NAME = "Creek";
    public static final String DEFAULT_TESTS_DIR_NAME = "src/system-test";
    public static final String DEFAULT_RESULTS_DIR_NAME =
            TestingBasePlugin.TEST_RESULTS_DIR_NAME + "/system-test";
    public static final Duration DEFAULT_EXPECTATION_TIMEOUT = Duration.ofMinutes(1);
    public static final String DEFAULT_SUITES_PATTERN = ".*";
    public static final String EXECUTOR_DEP_GROUP_NAME = "org.creekservice";
    public static final String EXECUTOR_DEP_ARTEFACT_NAME = "creek-system-test-executor";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(BasePlugin.class);

        final SystemTestExtension extension = registerExtension(project);
        registerSystemTestTask(project, extension);
        registerSystemTestEngineConfiguration(project);
    }

    private SystemTestExtension registerExtension(final Project project) {
        final SystemTestExtension extension =
                project.getExtensions().create(EXTENSION_NAME, SystemTestExtension.class);

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
        final SystemTestTask task =
                project.getTasks().create(SYSTEM_TEST_TASK_NAME, SystemTestTask.class);

        task.setGroup(GROUP_NAME);
        task.getTestDirectory().set(extension.getTestDirectory());
        task.getResultDirectory().set(extension.getResultDirectory());
        task.getExtraArguments().set(extension.getExtraArguments());
        task.getVerificationTimeoutSeconds().set(extension.getVerificationTimeoutSeconds());
        task.getSuitesPathPattern().set(extension.getSuitePathPattern());

        project.getTasksByName(LifecycleBasePlugin.CHECK_TASK_NAME, false)
                .forEach(checkTask -> checkTask.dependsOn(task));
    }

    private void registerSystemTestEngineConfiguration(final Project project) {
        final Configuration cfg = project.getConfigurations().create(CONFIGURATION_NAME);
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
                .withType(SystemTestTask.class)
                .configureEach(task -> task.getSystemTestDeps().from(cfg));
    }
}
