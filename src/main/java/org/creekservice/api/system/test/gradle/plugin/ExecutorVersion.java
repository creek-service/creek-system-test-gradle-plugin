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

package org.creekservice.api.system.test.gradle.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;

/**
 * Util to load the default executor version from the {@code creek-system-test-executor.version}
 * resource in the jar.
 */
public final class ExecutorVersion {

    private static final String VERSION_RESOURCE_NAME = "/creek-system-test-executor.version";

    private ExecutorVersion() {}

    /**
     * @return the default version of the test executor to use.
     */
    public static String defaultExecutorVersion() {
        return loadResource(VERSION_RESOURCE_NAME);
    }

    // @VisibleForTesting
    static String loadResource(final String resourceName) {
        try (InputStream resource = ExecutorVersion.class.getResourceAsStream(resourceName)) {
            if (resource == null) {
                throw new IllegalStateException(
                        "Jar does not contain " + resourceName + " resource");
            }

            return new String(resource.readAllBytes(), UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read " + resourceName + " resource", e);
        }
    }
}
