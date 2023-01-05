[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![build](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/build.yml)
[![Coverage Status](https://coveralls.io/repos/github/creek-service/creek-system-test-gradle-plugin/badge.svg?branch=main)](https://coveralls.io/github/creek-service/creek-system-test-gradle-plugin?branch=main)
[![CodeQL](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/creek-service/creek-system-test-gradle-plugin/badge)](https://api.securityscorecards.dev/projects/github.com/creek-service/creek-system-test-gradle-plugin)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/6842/badge)](https://bestpractices.coreinfrastructure.org/projects/6842)

# Creek System Test Gradle Plugin

A Gradle plugin for running Creek system tests as part of a Gradle build.

See [CreekService.org](https://www.creekservice.org) for info on Creek Service.

> ### NOTE
> The plugin works with Gradle 6.4 and above.

## Usage

To use the System Test plugin, include the following in your build script:

##### Groovy: Using the System Test plugin
```groovy
plugins {
    id 'org.creekservice.system.test' version '0.2.0'
}
```

##### Kotlin: Using the System Test plugin
```kotlin
plugins {
    id("org.creekservice.system.test") version "0.2.0"
}
```

Before running the system tests, any required container images should be built. 
This is best achieved by making the `systemTest` task depend on the tasks that build the images.
For example:

##### Groovy: Build service container images before running system tests
```groovy
tasks.systemTest {
    dependsOn ':example-service:buildAppImage'
}
```

##### Kotlin: Build service container images before running system tests
```kotlin
tasks.systemTest {
    dependsOn(":example-service:buildAppImage")
}
```

## Tasks

The System Test plugin adds the following tasks to your project:

### systemTest - [SystemTest][4]

> ### NOTE
> Details of how to write system tests can be found in the [Creek System Test Repo][1].

*Dependencies:* none. Users of this task should make the task dependent on the tasks the build the docker images under test.
*Dependants:* `check`

The `systemTest` task executes any system tests found in the project, by default under the `src/system-test` directory.

Aside from the customisations possible using the [`systemTest` extension](#system-test-extension), the task accepts the 
following command line options:

* `--verification-timeout-seconds=NUM`: (default: 60) the number of seconds the system test executor will wait for an 
   expectation to be met. Increasing the timeout will allow slow systems to be checked, at the expense of slower 
   test execution.
* `--include-suites=PATTERN`: (default: all) set a regular expression that can be used to filter which test suites to include.
   The relative path to a test suite must match the regular expression for it to be included.   
* `--extra-argument=ARG[=VALUE]`: (default: none) allows the passing of additional arguments to the test executor. 
  This can be useful, for example, to pass options to a newer version of the executor, which the plugin does not yet support.
* `--debug-service=NAME`: (default: none) the `NAME` of a service to debug when the system tests run.
  See [debugging system tests][debug-system-test] for more info.
* `--debug-service-instance=NAME`: (default: none) the `NAME` of a service instance to debug when the system tests run.
  See [debugging system tests][debug-system-test] for more info.

For example:
```bash
> gradlew systemTest \
    --verification-timeout-seconds=300 \
    --include-suites='.*/smoke/.*' \
    --extra-argument=--new-arg-one \
    --extra-argument=--new-arg-two=some-value \
    --debug-service=some-service \
    --debug-service-instance=some-service-2
```

### clean*TaskName* - `Delete`

Deletes the files created by the specified task. For example, `cleanSystemTest` will delete the test results.

## Project Layout

The System Test plugin assumes the project layout below. None of these directories need to exist or have anything in them. 
The plugin will run whatever it finds, and handles anything which is missing.

* `src/system-test`: Directory under which system test packages, suites and cases will be stored.

### Changing the project layout

You configure the project layout via the `creek.systemTest` configuration. This is discussed in more detail in the following
sections. Here is a brief example which changes the location under which system test packages are stored:

##### Groovy: Custom system test source directory
```groovy
creek {
    systemTest {
        testDirectory = file("$projectDir/custom-test")
    }
}
```

##### Kotlin: Custom system test source directory
```kotlin
creek {
    systemTest {
        // Set a custom location for the test packages:
        testDirectory.set(file("$projectDir/custom-test"))
    }
}
```

### Making system tests re-run on code changes

To ensure system tests run after code changes to the services under test, the `systemTest` task must be configured
to be dependent on the output of the task that creates each service's Docker image.

For example, if the system tests are in their own module, and the project uses the `com.bmuschko.docker-remote-api` 
plugin for building Docker images, then the following can be added to their Gradle build file
to have them depend on the Docker images of each service.  If the Docker image is rebuilt, the system tests not
be marked as 'up-to-date' by Gradle.

```kotlin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id("com.bmuschko.docker-remote-api") version "8.1.0"
}

tasks.systemTest {
    // Make the systemTest task be dependent on the output of all Docker image build tasks:
    rootProject.allprojects.flatMap {
       it.tasks.withType(DockerBuildImage::class)
    }.forEach {
        inputs.files(it)
    }
}
```

## Dependency Management

The System Test plugin adds a number of [dependency configurations][2] to your project.  Tasks such as `systemTest`
then use these configurations to get the corresponding files and use them, for example by adding them to the class path
when executing tests.

* `systemTestExecutor` the [system test executor][3] dependency, defaulting to the same version as the plugin.
* `systemTestExtension` system test extensions to allow the test to handle different types of resources.
* `systemTestComponent` additional dependencies containing the Aggregate and Service components involved in the tests. 

For example, the following is an example configuration for a repository containing its service descriptor in a 
`services` module and using the [creek-kafka test extension][kafka-test-ext]:

##### Groovy: setting system test dependencies
```groovy
dependencies {
    systemTestComponent project(':services')
    systemTestExtension 'org.creekservice:creek-kafka-test-extension:0.2.0'
}
```

##### Kotlin: setting system test dependencies
```kotlin
dependencies {
    systemTestComponent(project(":services"))
    systemTestExtension("org.creekservice:creek-kafka-test-extension:0.2.0")
}
```

### Changing the system test executor version

By default, the plugin executes system tests using the [system test executor][3] of the same version. However,
you can configure the executor version via the `systemTestExecutor` dependency configuration:

##### Groovy: Custom system test executor version
```groovy
dependencies {
    systemTestExecutor 'org.creekservice:creek-system-test-executor:0.2.0'
}
```

##### Kotlin: Custom system test executor version
```kotlin
dependencies {
    systemTestExecutor("org.creekservice:creek-system-test-executor:0.2.0")
}
```

When running a different version of the executor it may be that the executor supports command line options that
are not exposed by the plugin. In such situations, you can pass extra arguments to the executor using the
`extraArguments` method of the `creek.systemTest` extension.

##### Groovy: Passing extra arguments to the generator
```groovy
creek.systemTest {
    extraArguments "--some", "--extra=arguments"
}
```

##### Kotlin: Passing extra arguments to the generator
```kotlin
creek.systemTest {
    extraArguments("--some", "--extra=arguments")
}
```

## System Test Extension

The System Test plugin adds the `creek.systemTest` extension. This allows you to configure a number of task related properties
inside a dedicated DSL block.

For more information on service debugging, see [debugging system tests][debug-system-test].

##### Groovy: Using the `systemTest` extension
```groovy
creek.systemTest {
    // (Optional) Set a custom location for the test packages:
    // Default: src/system-test
    testDirectory = file("$projectDir/custom-test")
    
    // (Optional) Set a custom location for results to be written:
    // Default : build/test-results/system-test
    resultDirectory = file("$buildDir/custom-result")
    
    // (Optional) Set a custom verification timeout:
    // Default: 1 minute
    verificationTimeout Duration.ofMinutes(2)
    
    // (Optional) Set a filter to limit which test suites to run:
    // Default: all suites
    suitePathPattern = ".*include.*"

    // (Optional) Set extra arguments to be used when running system tests:
    // Default: none
    extraArguments "--some", "--extra=arguments"
    
    // Optional configuration of service debugging during system test runs
    debugging {
        // (Optional) Set the port the AttachMe IntelliJ plugin is listening on.
        // This can be configured in the `AttachMe` run configuration in Intelli
        // Default: 7857 (The plugin's default)
        attachMePort = 1234
        
        // (Optional) Set the base port number services will expose to the debugger.
        // The first service instance being debugged will listen for the debugger attaching on this port number.
        // Subsequent instances will listen on sequential ports.
        // Default: 8000
        baseServicePort = 4321
        
        // (Optional) The set of services to debug
        // All instances of the service will be debugged.
        // Default: none
        serviceNames "service-a", "service-b"

        // (Optional) The set of service instances to debug.
        // Instance names are in the form <service-name>-<instance-number>
        // Instance number starts at zero for the first instance of the service to be started.
        // Default: none
        serviceInstanceNames "service-a-0"
    }
}
```

##### Kotlin: Using the `systemTest` extension
```kotlin
creek.systemTest {
    // (Optional) Set a custom location for the test packages:
    // Default: src/system-test
    testDirectory.set(file("$projectDir/custom-test"))
    
    // (Optional) Set a custom location for results to be written:
    // Default : build/test-results/system-test
    resultDirectory.set(file("$buildDir/custom-result"))

    // (Optional) Set a custom verification timeout:
    // Default: 1 minute
    verificationTimeout(Duration.ofMinutes(2))

    // (Optional) Set a filter to limit which test suites to run:
    // Default: all suites
    suitePathPattern.set(".*include.*")

    // (Optional) Set extra arguments to be used when running system tests:
    // Default: none
    extraArguments("--some", "--extra=arguments")

    // Optional configuration of service debugging during system test runs
    debugging {
        // (Optional) Set the port the AttachMe IntelliJ plugin is listening on.
        // This can be configured in the `AttachMe` run configuration in Intelli
        // Default: 7857 (The plugin's default)
        attachMePort.set(1234)

        // (Optional) Set the base port number services will expose to the debugger.
        // The first service instance being debugged will listen for the debugger attaching on this port number.
        // Subsequent instances will listen on sequential ports.
        // Default: 8000
        baseServicePort.set(4321)

        // (Optional) The set of services to debug
        // All instances of the service will be debugged.
        // Default: none
        serviceNames.set(setOf("service-a", "service-b"))

        // (Optional) The set of service instances to debug.
        // Instance names are in the form <service-name>-<instance-number>
        // Instance number starts at zero for the first instance of the service to be started.
        // Default: none
        serviceInstanceNames.set(setOf("instance-c", "instance-d"))
    }
}
```

## Test Reporting

The `systemTest` task generates XML JUnit style test results. 
By default, these are written to `$buildDir/test-results/system-test`. The output location can be changed by setting
the `creek.systemTest.resultDirectory` property. 

## Debugging system tests

Creek supports debugging of the services running in their Docker containers.
Service debugging requires the IntelliJ [AttachMe][attachMe] plugin to be installed.

With the [AttachMe][attachMe] plugin installed, debugging a service is as simple as 1, 2, 3:

1. Create and run an `AttachMe` run configuration on the default port of `7857`.
2. Place the required breakpoints in the service's code.
3. Run the system tests with additional `--debug-service` arguments. For example:
   ```
   ./gradlew systemTest --debug-service=some-service
   ```

When the named service is started, the debugger will attach.

For more details on system test debugging, see the [creek-system-test][debug-system-test] docs.

[1]: https://github.com/creek-service/creek-system-test
[2]: https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:what-are-dependency-configurations
[3]: https://github.com/creek-service/creek-system-test/tree/main/executor
[4]: src/main/java/org/creekservice/api/system/test/gradle/plugin/test/SystemTest.java
[kafka-test-ext]: https://github.com/creek-service/creek-kafka/tree/main/test-extension
[debug-system-test]: https://github.com/creek-service/creek-system-test#debugging-system-tests
[attachMe]: https://plugins.jetbrains.com/plugin/13263-attachme
