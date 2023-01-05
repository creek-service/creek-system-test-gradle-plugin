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

package org.creekservice.api.system.test.gradle.plugin.debug;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.GROUP_NAME;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.MOUNT_DIR;
import static org.creekservice.api.system.test.gradle.plugin.debug.AttachMeAgentJarFinder.findAttacheMeAgentJar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Task for setting up a mount directory storing the AttachMe debug agent. */
public abstract class PrepareDebug extends DefaultTask {

    /**
     * Create task
     *
     * @param project the project being configured.
     */
    @Inject
    public PrepareDebug(final Project project) {
        setGroup(GROUP_NAME);

        getAttachMeDirectory()
                .convention(
                        project.getLayout()
                                .dir(
                                        project.provider(
                                                () ->
                                                        Paths.get(System.getProperty("user.home"))
                                                                .resolve(".attachme")
                                                                .toFile())));
        getMountDirectory()
                .convention(project.getLayout().getBuildDirectory().dir(MOUNT_DIR + "/debug"));

        onlyIf(t -> getAttachMeDirectory().get().getAsFile().exists());
    }

    /**
     * @return the local directory where the AttachMe agent is located.
     */
    @InputDirectory
    public abstract DirectoryProperty getAttachMeDirectory();

    /**
     * @return The directory under which the JaCoCo agent jar should be stored.
     */
    @OutputDirectory
    public abstract DirectoryProperty getMountDirectory();

    /**
     * @return the agent jar filename, relative to the {@link #getMountDirectory() mount directory},
     *     if present.
     */
    @Internal
    public Optional<Path> getAgentJarFileName() {
        final Directory dir = getMountDirectory().get();
        final FileTree files = dir.getAsFileTree();
        if (files.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(dir.getAsFile().toPath().relativize(files.getSingleFile().toPath()));
    }

    /** Run the task. */
    @TaskAction
    public void run() {
        final Path attachMeDir = getAttachMeDirectory().get().getAsFile().toPath().toAbsolutePath();
        final Path mountDir = getMountDirectory().get().getAsFile().toPath().toAbsolutePath();
        createMountDir(mountDir);
        copyAgentJar(mountDir, attachMeDir);
    }

    private void createMountDir(final Path mountDir) {
        try {
            Files.createDirectories(mountDir);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create mount directory: " + mountDir, e);
        }
    }

    private void copyAgentJar(final Path mountDir, final Path attachMeDir) {
        findAttacheMeAgentJar(attachMeDir)
                .ifPresent(
                        agentJar -> {
                            try {
                                Files.copy(
                                        agentJar,
                                        mountDir.resolve(agentJar.getFileName()),
                                        REPLACE_EXISTING);
                            } catch (final IOException e) {
                                throw new RuntimeException(
                                        "Failed to copy agent jar: " + agentJar, e);
                            }
                        });
    }
}
