[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![build](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/gradle.yml)
[![Coverage Status](https://coveralls.io/repos/github/creek-service/creek-system-test-gradle-plugin/badge.svg?branch=main)](https://coveralls.io/github/creek-service/creek-system-test-gradle-plugin?branch=main)
[![CodeQL](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/creek-service/creek-system-test-gradle-plugin/actions/workflows/codeql.yml)

# Creek System Test Gradle Plugin

A Gradle plugin for running Creek system tests as part of a Gradle build.

> ## NOTE
> The plugin works with Gradle 6.4 and above.

If following standard patterns, for example storing test packages under `src/system-test`, then simply applying
the plugin should be sufficient:

##### Groovy: build.gradle
```groovy
plugins {
    id 'org.creekservice.system.test'
}
```

##### Kotlin: build.gradle.kts
```kotlin
plugins {
    id("org.creekservice.system.test")
}
```

This will add the tasks below. Additionally, the `systemTest` task will be added as a dependency of the standard `check` 
task, meaning running `check` will also run the system tests.  The results of the system tests will be written out
as JUnit compatible test reports, with one test suite per file. By default, results are written to the 
`build/test-results/system-test` directory.

## Tasks

The plugin defines the following tasks:

### `systemTest` Task

The `systemTest` task executes the system tests. Aside from the customisations possible using the 
[`systemTest` extension](#extension), the task accepts the following command line options:

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

## Extension

The plugin defines a `systemTest` extension that can be used to customise the plugin's tasks. 

##### Groovy: build.gradle
```groovy
plugins {
    id 'org.creekservice.system.test'
}

systemTest {
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

##### Kotlin: build.gradle.kts
```kotlin
plugins {
    id("org.creekservice.system.test")
}

systemTest {
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

## Configuration

The plugin defines a `systemTest` configuration that can be used control which version of the system test executor to
use. 

##### Groovy: build.gradle
```groovy
plugins {
    id 'org.creekservice.system.test'
}

dependencies {
    systemTest 'org.creek:creek-system-test-executor:0.1.13'
}
```

##### Kotlin: build.gradle.kts
```kotlin
plugins {
    id("org.creekservice.system.test")
}

dependencies {
    systemTest("org.creek:creek-system-test-executor:0.1.13")
}
```
