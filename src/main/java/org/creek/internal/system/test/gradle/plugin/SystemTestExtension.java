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


import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public abstract class SystemTestExtension {

    /** @return the directory to search for test packages. */
    public abstract DirectoryProperty getTestDirectory();

    /** @return the directory under which test result files will be written. */
    public abstract DirectoryProperty getResultDirectory();

    /** @return how long to wait for expectations to be met */
    public abstract Property<String> getVerificationTimeoutSeconds(); // Todo: Duration?

    // @return Optional pattern used to limit which test suites to run. A suite's relative path must
    // match the supplied regular expression. */
    public abstract Property<String> getSuitePathPattern();

    /** @return list of additional arguments to pass to the test executor */
    public abstract ListProperty<String> getAdditionalExecutorArguments();
}

// Todo: document
