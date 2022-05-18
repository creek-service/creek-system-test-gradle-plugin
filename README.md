[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![build](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/gradle.yml)
[![Coverage Status](https://coveralls.io/repos/github/creek-service/creek-system-test-gradle-plugin/badge.svg?branch=main)](https://coveralls.io/github/creek-service/creek-system-test-gradle-plugin?branch=main)
[![CodeQL](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/codeql.yml)

# Creek System Test Gradle Plugin

A Gradle plugin for running Creek system tests as part of a Gradle build.

> ### NOTE
> The plugin works with Gradle 6.4 and above.

## Usage

To use the System Test plugin, include the following in your build script:

##### Groovy: Using the System Test plugin
```groovy
plugins {
    id 'org.creekservice.system.test'
}
```

##### Kotlin: Using the System Test plugin
```kotlin
plugins {
    id("org.creekservice.system.test")
}
```

## Tasks

The System Test plugin adds the following tasks to your project:

### systemTest - [SystemTest][4]

> ### NOTE
> Details of how to write system tests can be found in the [Creek System Test Repo][1].

*Dependencies:* none. Users of this task should make the task dependent on the tasks the build the docker images under test.
*Dependants:* `check`

The `systemTest` task executes any system tests found in the project. 

Aside from the customisations possible using the [`systemTest` extension](#system-test-extension), the task accepts the 
following command line options:

* `--verification-timeout-seconds`: (default: 60) the number of seconds the system test executor will wait for an 
   expectation to be met. Increasing the timeout will allow slow systems to be checked, at the expense of slower 
   test execution.
* `--include-suites`: (default: all) set a regular expression that can be used to filter which test suites to include.
   The relative path to a test suite must match the regular expression for it to be included.   
* `--extra-argument`: (default: none) allows the passing of additional arguments to the test executor. This can be 
  useful, for example, to pass options to a newer version of the executor, which the plugin does not yet support.

For example:
```bash
> gradlew systemTest \
    --verification-timeout-seconds=300 \
    --include-suites='.*/smoke/.*' \
    --extra-argument=--new-arg-one \
    --extra-argument=--new-arg-two=some-value
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

## Dependency Management

The System Test plugin adds a number of [dependency configurations][2] to your project.  Tasks such as `systemTest`
then use these configurations to get the corresponding files and use them, for example by adding them to the class path
when executing tests.

* `systemTestExecutor` the [system test executor][3] dependency, defaulting to the same version as the plugin.
* `systemTestExtension` system test extensions to allow the test to handle different types of resources.
* `systemTestComponent` additional dependencies containing the Aggregate and Service components involved in the tests. 

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

##### Groovy: Using the `systemTest` extension
```groovy
creek.systemTest {
    // Set a custom location for the test packages:
    testDirectory = file("$projectDir/custom-test")
    
    // Set a custom location for results to be written:
    resultDirectory = file("$buildDir/custom-result")
    
    // Set a custom verification timeout:
    verificationTimeout Duration.ofMinutes(2)
    
    // Set a filter to limit which test suites to run: 
    suitePathPattern = ".*include.*"

    // Set extra arguments to be used when running system tests:
    extraArguments "--some", "--extra=arguments"
}
```

##### Kotlin: Using the `systemTest` extension
```kotlin
creek.systemTest {
    // Set a custom location for the test packages:
    testDirectory.set(file("$projectDir/custom-test"))
    
    // Set a custom location for results to be written:
    resultDirectory.set(file("$buildDir/custom-result"))

    // Set a custom verification timeout:
    verificationTimeout(Duration.ofMinutes(2))

    // Set a filter to limit which test suites to run:
    suitePathPattern.set(".*include.*")

    // Set extra arguments to be used when running system tests:
    extraArguments("--some", "--extra=arguments")
}
```

## Test Reporting

The `systemTest` task generates XML JUnit style test results. 
By default, these are written to `$buildDir/test-results/system-test`. The output location can be changed by setting
the `creek.systemTest.resultDirectory` property. 

[1]: https://github.com/creek-service/creek-system-test
[2]: https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:what-are-dependency-configurations
[3]: https://github.com/creek-service/creek-system-test/tree/main/executor
[4]: src/main/java/org/creekservice/api/system/test/gradle/plugin/task/SystemTest.java