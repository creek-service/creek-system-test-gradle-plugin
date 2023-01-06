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

package org.creekservice.api.system.test.gradle.plugin.coverage;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.GROUP_NAME;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.HOST_MOUNT_DIR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;

/** Task for setting up a mount directory storing the Jacoco agent. */
public abstract class PrepareCoverage extends DefaultTask {

    /**
     * Create task
     *
     * @param project the project being configured.
     */
    @Inject
    public PrepareCoverage(final Project project) {
        setGroup(GROUP_NAME);

        getMountDirectory()
                .set(project.getLayout().getBuildDirectory().dir(HOST_MOUNT_DIR + "jacoco"));

        onlyIf(t -> jacocoAgentConfig() != null);
    }

    /**
     * @return The directory under which the JaCoCo agent jar should be stored.
     */
    @OutputDirectory
    public abstract DirectoryProperty getMountDirectory();

    /**
     * Run the task.
     *
     * @throws IOException on failed file operations
     */
    @TaskAction
    public void run() throws IOException {
        final Path mountDir = getMountDirectory().get().getAsFile().toPath().toAbsolutePath();
        final Configuration jacocoAgentConf = jacocoAgentConfig();
        createMountDir(mountDir);
        copyAgentJar(mountDir, jacocoAgentConf);
    }

    /**
     * @return the agent jar filename, relative to the {@link #getMountDirectory() mount directory},
     *     if present.
     */
    @Internal
    public Optional<Path> getAgentJarFileName() {
        final Directory dir = getMountDirectory().get();
        final FileTree files = dir.getAsFileTree();
        return Optional.of(dir.getAsFile().toPath().relativize(files.getSingleFile().toPath()));
    }

    private void createMountDir(final Path mountDir) throws IOException {
        Files.createDirectories(mountDir);
    }

    private void copyAgentJar(final Path mountDir, final Configuration jacocoAgentConf)
            throws IOException {
        final Path agentJar = extractAgentJar(jacocoAgentConf);
        Files.copy(agentJar, mountDir.resolve(agentJar.getFileName()), REPLACE_EXISTING);
    }

    private Path extractAgentJar(final Configuration jacocoAgentConf) {
        return getProject()
                .zipTree(jacocoAgentConf.getSingleFile())
                .filter(f -> f.getName().endsWith(".jar"))
                .getSingleFile()
                .toPath();
    }

    private Configuration jacocoAgentConfig() {
        return getProject().getConfigurations().findByName(JacocoPlugin.AGENT_CONFIGURATION_NAME);
    }
}
