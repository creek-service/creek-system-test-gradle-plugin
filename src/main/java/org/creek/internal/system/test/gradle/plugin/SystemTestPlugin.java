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

package org.creek.internal.system.test.gradle.plugin;


import java.time.Duration;
import java.util.List;
import org.creek.internal.system.test.gradle.plugin.task.SystemTestTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public final class SystemTestPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "systemTest"; // Todo: document
    private static final String CONFIGURATION_NAME = "systemTest"; // Todo: document
    private static final String GROUP_NAME = "Creek";
    private static final String DEFAULT_TESTS_DIR = "src/system-test";
    private static final String DEFAULT_RESULTS_DIR = "test-results/system-test";
    private static final Duration DEFAULT_EXPECTATION_TIMEOUT = Duration.ofMinutes(1);
    private static final String DEFAULT_SUITES_PATTERN = ".*";

    @Override
    public void apply(final Project project) {
        registerTasks(project);
    }

    private void registerTasks(final Project project) {
        final SystemTestExtension extension = registerExtension(project);
        registerSystemTestTask(project, extension);
        registerSystemTestEngineConfiguration(project);
    }

    private SystemTestExtension registerExtension(final Project project) {
        final SystemTestExtension extension =
                project.getExtensions().create(EXTENSION_NAME, SystemTestExtension.class);

        extension
                .getTestDirectory()
                .convention(project.getLayout().getProjectDirectory().dir(DEFAULT_TESTS_DIR));
        extension
                .getResultDirectory()
                .convention(project.getLayout().getBuildDirectory().dir(DEFAULT_RESULTS_DIR));
        extension
                .getVerificationTimeoutSeconds()
                .convention(String.valueOf(DEFAULT_EXPECTATION_TIMEOUT.toSeconds()));
        extension.getSuitePathPattern().convention(DEFAULT_SUITES_PATTERN);
        extension.getAdditionalExecutorArguments().convention(List.of());
        return extension;
    }

    private void registerSystemTestTask(
            final Project project, final SystemTestExtension extension) {
        final SystemTestTask task = project.getTasks().create("systemTest", SystemTestTask.class);

        task.setGroup(GROUP_NAME);
        task.getTestDirectory().set(extension.getTestDirectory());
        task.getResultDirectory().set(extension.getResultDirectory());
        task.getAdditionalExecutorArguments().set(extension.getAdditionalExecutorArguments());
        task.getVerificationTimeoutSeconds().set(extension.getVerificationTimeoutSeconds());
        task.getSuitesPathPattern().set(extension.getSuitePathPattern());
    }

    private void registerSystemTestEngineConfiguration(final Project project) {
        final Configuration cfg = project.getConfigurations().create(CONFIGURATION_NAME);
        cfg.setVisible(false);
        cfg.setCanBeConsumed(false);
        cfg.setCanBeResolved(true);
        cfg.setDescription("Dependency for the system test executor");

        // Todo: Need to code to specific version... but how?
        final String pluginDep = "org.creek:creek-system-test-executor:+";
        final DependencyHandler projectDeps = project.getDependencies();
        cfg.defaultDependencies(
                deps -> deps.add(projectDeps.create(pluginDep)));

        project.getTasks()
                .withType(SystemTestTask.class)
                .configureEach(task -> task.getSystemTestDeps().from(cfg));
    }
}

// Todo: test
