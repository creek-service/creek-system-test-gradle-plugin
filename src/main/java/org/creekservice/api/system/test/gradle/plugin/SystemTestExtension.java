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

import java.time.Duration;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/** Extension for configuring Creek system tests. */
public abstract class SystemTestExtension {

    private final DebugExtension debugExt;

    /**
     * @param objectFactory the @{link ObjectFactory} to use.
     */
    @Inject
    public SystemTestExtension(final ObjectFactory objectFactory) {
        this.debugExt = objectFactory.newInstance(DebugExtension.class);
        debugExt.getAttachMePort().convention(DebugExtension.DEFAULT_ATTACH_ME_PORT);
        debugExt.getBaseServicePort().convention(DebugExtension.DEFAULT_BASE_DEBUG_PORT);
    }

    /**
     * @return the directory to search for test packages.
     */
    public abstract DirectoryProperty getTestDirectory();

    /**
     * @return the directory under which test result files will be written.
     */
    public abstract DirectoryProperty getResultDirectory();

    /**
     * @return how long to wait for expectations to be met
     */
    public abstract Property<String> getVerificationTimeoutSeconds();

    /**
     * Set the verification timeout.
     *
     * <p>The verification timeout is that amount of time to wait for all expectations to be met.
     *
     * @param timeout the timeout.
     */
    @SuppressWarnings("unused") // Invoked from Gradle
    public void verificationTimeout(final Duration timeout) {
        getVerificationTimeoutSeconds().set(String.valueOf(timeout.toSeconds()));
    }

    /**
     * @return Optional pattern used to limit which test suites to run. A suite's relative path must
     *     match the supplied regular expression.
     */
    public abstract Property<String> getSuitePathPattern();

    /**
     * @return list of additional arguments to pass to the test executor
     *     <p>See <a
     *     href="https://github.com/creek-service/creek-system-test/tree/main/executor">Executor
     *     docs</a> for more info.
     */
    public abstract ListProperty<String> getExtraArguments();

    /**
     * Set additional args to pass to the test executor.
     *
     * <p>See <a
     * href="https://github.com/creek-service/creek-system-test/tree/main/executor">Executor
     * docs</a> for more info.
     *
     * @param args the extra args to use when running system tests.
     */
    @SuppressWarnings("unused") // Invoked from Gradle
    public void extraArguments(final String... args) {
        getExtraArguments().set(List.of(args));
    }

    /**
     * @return the debugging extension *
     */
    public DebugExtension getDebugging() {
        return debugExt;
    }

    /**
     * Configure debugging extension
     *
     * @param action the action to perform on the debugging ext.
     */
    @SuppressWarnings("unused") // Invoked from Gradle
    public void debugging(final Action<DebugExtension> action) {
        action.execute(debugExt);
    }

    // Avoid finalizer attacks: spotbugs CT_CONSTRUCTOR_THROW
    @SuppressWarnings("deprecation")
    @Override
    protected final void finalize() {}
}
