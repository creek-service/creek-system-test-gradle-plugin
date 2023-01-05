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


import java.util.Set;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/** Extension for configuring information about debugging. */
public abstract class DebugExtension {

    /** The default attachMe port that the IntelliJ attachMe plugin uses. */
    public static final int DEFAULT_ATTACH_ME_PORT = 7857;

    /**
     * The start of the default range of ports on the local machine that services will listen on for
     * the debugger.
     */
    public static final int DEFAULT_BASE_DEBUG_PORT = 8000;

    /**
     * The port on which the attachMe plugin is listening on.
     *
     * <p>This is the port the attachMe agent running within the microservice's process will call
     * out on to ask the debugger to attach.
     *
     * @return the port the attachMe plugin is listening on.
     */
    public abstract Property<Integer> getAttachMePort();

    /**
     * The base debug port.
     *
     * <p>The port the first service being debugged will listen on for the debugger to attach.
     * Subsequent services being debugged will use sequential port numbers.
     *
     * @return the base port number used for debugging.
     */
    public abstract Property<Integer> getBaseServicePort();

    /**
     * The set of services to be debugged.
     *
     * @return set of services to debug.
     */
    public abstract SetProperty<String> getServiceNames();

    /**
     * Set service names to debug
     *
     * @param names the service names to debug
     */
    @SuppressWarnings("unused") // Invoked from Gradle
    public void serviceNames(final String... names) {
        getServiceNames().set(Set.of(names));
    }

    /**
     * The set of service instances to be debugged.
     *
     * <p>An instance name is the name of the service with a dash and the instance number appended,
     * e.g. {@code my-service-1}.
     *
     * @return set of service instances to debug.
     */
    public abstract SetProperty<String> getServiceInstanceNames();

    /**
     * Set service names to debug
     *
     * @param names the service instance names to debug
     */
    @SuppressWarnings("unused") // Invoked from Gradle
    public void serviceInstanceNames(final String... names) {
        getServiceInstanceNames().set(Set.of(names));
    }
}
