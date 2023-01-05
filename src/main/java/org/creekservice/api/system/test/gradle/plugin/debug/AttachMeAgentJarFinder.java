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

package org.creekservice.api.system.test.gradle.plugin.debug;


import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.stream.Stream;

/** Util class for finding the AttachMe agent jar */
final class AttachMeAgentJarFinder {

    private static final PathMatcher JAR_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.jar");

    private AttachMeAgentJarFinder() {}

    public static Optional<Path> findAttacheMeAgentJar(final Path attachMeDir) {
        try (Stream<Path> stream = Files.list(attachMeDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(JAR_MATCHER::matches)
                    .filter(p -> p.getFileName().toString().startsWith("attachme-agent-"))
                    .sorted()
                    .reduce((l, r) -> r);
        } catch (final IOException e) {
            return Optional.empty();
        }
    }
}
