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

import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.CONTAINER_MOUNT_DIR;
import static org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin.HOST_MOUNT_DIR;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.creekservice.api.system.test.gradle.plugin.SystemTestPlugin;
import org.creekservice.api.system.test.gradle.plugin.test.SystemTest;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;

/**
 * Extension applied to system test tasks if the JaCoCo plugin is installed.
 *
 * <p>Handles configuration of coverage options.
 */
public class SystemTestCoverageExtension {

    /** Name of coverage task extension. */
    public static final String COVERAGE_EXT_NAME = "coverage";

    /**
     * The path within the container where a read-only directory containing the Jacoco agent will be
     * mounted
     */
    public static final String CONTAINER_AGENT_MOUNT = CONTAINER_MOUNT_DIR + "jacoco/";

    /**
     * The path within the container where a writable directory will be mounted to receive the
     * coverage results.
     */
    public static final String CONTAINER_RESULT_MOUNT = CONTAINER_MOUNT_DIR + "coverage/";

    private final PrepareCoverage prepareTask;
    private final Property<String> resultFileName;
    private final DirectoryProperty mountDir;

    /**
     * Create extension, attaching it to the supplied {@code task}.
     *
     * @param task the test the extension is being applied to.
     */
    public SystemTestCoverageExtension(final SystemTest task) {
        this.prepareTask = prepareCoverageTask(task.getProject());
        this.resultFileName = task.getProject().getObjects().property(String.class);
        this.resultFileName.convention(task.getName() + ".exec");
        this.mountDir = task.getProject().getObjects().directoryProperty();
        this.mountDir.convention(
                task.getProject().getLayout().getBuildDirectory().dir(HOST_MOUNT_DIR + "coverage"));
    }

    /**
     * @return the file name, with in the {@link #getResultMountDirectory() mount directory}, where
     *     coverage results will be written.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "intentional")
    @Input
    public Property<String> getResultFileName() {
        return resultFileName;
    }

    /**
     * @return The directory where results will be written, which will be mounted into the Docker
     *     container.
     */
    @OutputDirectory
    public DirectoryProperty getResultMountDirectory() {
        return mountDir;
    }

    /**
     * @return the path to where the execution data is written.
     */
    @Internal
    public File getDestinationFile() {
        return getResultMountDirectory().get().file(getResultFileName().get()).getAsFile();
    }

    /**
     * @return command line arguments to pass to the test executor to set up the coverage mounts.
     */
    public List<String> mountOptions() {
        return List.of(
                "--mount-read-only="
                        + prepareTask.getMountDirectory().get()
                        + "="
                        + CONTAINER_AGENT_MOUNT,
                "--mount-writable="
                        + getResultMountDirectory().get()
                        + "="
                        + CONTAINER_RESULT_MOUNT);
    }

    /**
     * @return the text to place into the {@code JAVA_TOOL_OPTIONS} environment variable to have
     *     coverage captured.
     */
    public String asJavaToolOptions() {
        final Path agentJarFileName =
                prepareTask
                        .getAgentJarFileName()
                        .orElseThrow(() -> new IllegalStateException("No Jacoco agent jar found."));

        return "-javaagent:"
                + CONTAINER_AGENT_MOUNT
                + agentJarFileName
                + "=destfile="
                + CONTAINER_RESULT_MOUNT
                + getResultFileName().get()
                + ",append=true,inclnolocationclasses=false,dumponexit=true,output=file,jmx=false";
    }

    /** Remove any previous results */
    public void cleanUp() {
        try {
            Files.deleteIfExists(
                    getResultMountDirectory()
                            .getAsFile()
                            .get()
                            .toPath()
                            .resolve(getResultFileName().get()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrepareCoverage prepareCoverageTask(final Project project) {
        return (PrepareCoverage)
                project.getTasksByName(SystemTestPlugin.PREPARE_COVERAGE_TASK_NAME, false)
                        .iterator()
                        .next();
    }
}
