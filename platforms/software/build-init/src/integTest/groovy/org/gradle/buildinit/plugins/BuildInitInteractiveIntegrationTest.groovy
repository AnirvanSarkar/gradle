/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.buildinit.plugins

import org.gradle.buildinit.plugins.fixtures.ScriptDslFixture
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl
import org.gradle.internal.jvm.Jvm
import org.gradle.test.fixtures.ConcurrentTestUtil
import org.gradle.util.internal.TextUtil
import spock.lang.Issue

class BuildInitInteractiveIntegrationTest extends AbstractInitIntegrationSpec {

    def buildTypePrompt = "Select type of build to generate:"
    def dslPrompt = "Select build script DSL:"
    def incubatingPrompt = "Generate build using new APIs and behavior (some features may change in the next minor release)?"
    def basicType = "4: Basic (build structure only)"
    def basicTypeOption = 4
    def applicationOption = 1
    def projectNamePrompt = "Project name (default: some-thing)"
    def convertMavenBuildPrompt = "Found a Maven build. Generate a Gradle build from this?"
    def javaOption = 1
    def languageSelectionOptions = [
        "Select implementation language:",
        "1: Java",
        "2: Kotlin",
        "3: Groovy",
        "4: Scala",
        "5: C++",
        "6: Swift"
    ]

    @Override
    String subprojectName() { 'app' }

    def "prompts user when run from an interactive session"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init")
        def handle = executer.start()

        // Select 'basic'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(buildTypePrompt)
            assert handle.standardOutput.contains("1: Application")
            assert handle.standardOutput.contains("2: Library")
            assert handle.standardOutput.contains("3: Gradle plugin")
            assert handle.standardOutput.contains(basicType)
            assert !handle.standardOutput.contains("pom")
        }
        handle.stdinPipe.write((basicTypeOption + TextUtil.platformLineSeparator).bytes)

        // Select 'kotlin'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(dslPrompt)
            assert handle.standardOutput.contains("1: Kotlin")
            assert handle.standardOutput.contains("2: Groovy")
        }
        handle.stdinPipe.write(("1" + TextUtil.platformLineSeparator).bytes)

        // Select default project name
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(projectNamePrompt)
        }
        handle.stdinPipe.write(TextUtil.platformLineSeparator.bytes)

        // Select 'no' for incubating APIs
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(incubatingPrompt)
        }
        handle.stdinPipe.write(("no" + TextUtil.platformLineSeparator).bytes)

        // after generating the project, we suggest the user reads some documentation
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(documentationRegistry.getSampleForMessage())
        }
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        ScriptDslFixture.of(BuildInitDsl.KOTLIN, targetDir, null).assertGradleFilesGenerated()
    }

    def "does not prompt for options provided on the command-line"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init", "--incubating", "--dsl", "kotlin", "--type", "basic")
        def handle = executer.start()

        // Select default project name
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(projectNamePrompt)
        }
        handle.stdinPipe.write(TextUtil.platformLineSeparator.bytes)

        // after generating the project, we suggest the user reads some documentation
        def msg = documentationRegistry.getSampleForMessage()
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(msg)
        }
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        ScriptDslFixture.of(BuildInitDsl.KOTLIN, targetDir, null).assertGradleFilesGenerated()
    }

    def "user can provide details for Java build"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init")
        def handle = executer.start()

        // Select 'application'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(buildTypePrompt)
        }
        handle.stdinPipe.write((applicationOption + TextUtil.platformLineSeparator).bytes)

        // Select 'java'
        ConcurrentTestUtil.poll(60) {
            languageSelectionOptions.each {
                assert handle.standardOutput.contains(it)
            }
        }
        handle.stdinPipe.write((javaOption + TextUtil.platformLineSeparator).bytes)

        // Enter a Java version
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains("Enter target Java version (min: 7, default: 21)")
        }
        handle.stdinPipe.write(("17" + TextUtil.platformLineSeparator).bytes)

        // Select 'Single project'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains("Generate multiple subprojects for application?")
        }
        handle.stdinPipe.write(("no" + TextUtil.platformLineSeparator).bytes)

        // Select 'kotlin' DSL
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(dslPrompt)
        }
        handle.stdinPipe.write(("1" + TextUtil.platformLineSeparator).bytes)

        // Select 'junit'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains("Select test framework:")
            assert handle.standardOutput.contains("1: JUnit 4")
        }
        handle.stdinPipe.write(("1" + TextUtil.platformLineSeparator).bytes)

        // Select default project name
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(projectNamePrompt)
        }
        handle.stdinPipe.write(TextUtil.platformLineSeparator.bytes)

        // Select 'no' for incubating APIs
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(incubatingPrompt)
        }
        handle.stdinPipe.write(("no" + TextUtil.platformLineSeparator).bytes)

        // after generating the project, we suggest the user reads some documentation
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(documentationRegistry.getSampleForMessage())
        }
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        dslFixtureFor(BuildInitDsl.KOTLIN).assertGradleFilesGenerated()
    }

    def "user can interrupt the build without generating files"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init")
        def handle = executer.start()

        // Interrupt input
        handle.stdinPipe.close()

        def result = handle.waitForFailure()

        then:
        result.assertHasDescription("Execution failed for task ':init'.")
        result.assertHasCause("Build cancelled.")
        rootProjectDslFixtureFor(BuildInitDsl.GROOVY).assertGradleFilesNotGenerated()
    }

    def "user can interrupt the build after multiple prompts without generating files"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init")
        def handle = executer.start()

        // Select 'application'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(buildTypePrompt)
        }
        handle.stdinPipe.write((applicationOption + TextUtil.platformLineSeparator).bytes)

        // Select 'java'
        ConcurrentTestUtil.poll(60) {
            languageSelectionOptions.each {
                assert handle.standardOutput.contains(it)
            }
        }
        handle.stdinPipe.write((javaOption + TextUtil.platformLineSeparator).bytes)

        // Interrupt input
        handle.stdinPipe.close()

        def result = handle.waitForFailure()

        then:
        result.assertHasDescription("Execution failed for task ':init'.")
        result.assertHasCause("Build cancelled.")
        rootProjectDslFixtureFor(BuildInitDsl.GROOVY).assertGradleFilesNotGenerated()
    }

    def "prompts user when run from an interactive session and pom.xml present"() {
        when:
        pom()

        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init")
        def handle = executer.start()

        // Select 'yes'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(convertMavenBuildPrompt)
        }
        handle.stdinPipe.write(TextUtil.platformLineSeparator.bytes)

        // Select 'groovy' DSL
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(dslPrompt)
        }
        handle.stdinPipe.write(("2" + TextUtil.platformLineSeparator).bytes)

        // Select 'no' for incubating APIs
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(incubatingPrompt)
        }
        handle.stdinPipe.write(("no" + TextUtil.platformLineSeparator).bytes)

        // after generating the project, we suggest the user reads some documentation
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(documentationRegistry.getDocumentationRecommendationFor("information", "migrating_from_maven"))
        }
        handle.stdinPipe.close()
        handle.waitForFinish()

        !handle.standardOutput.contains(buildTypePrompt)
        !handle.standardOutput.contains(dslPrompt)
        !handle.standardOutput.contains(projectNamePrompt)

        then:
        rootProjectDslFixtureFor(BuildInitDsl.GROOVY).assertGradleFilesGenerated()
    }

    def "user can skip Maven conversion when pom.xml present"() {
        when:
        pom()

        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks("init")
        def handle = executer.start()

        // Select 'no'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(convertMavenBuildPrompt)
        }
        handle.stdinPipe.write(("no" + TextUtil.platformLineSeparator).bytes)

        // Select 'basic'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(buildTypePrompt)
            assert handle.standardOutput.contains(basicType)
            assert !handle.standardOutput.contains("pom")
        }
        handle.stdinPipe.write((basicTypeOption + TextUtil.platformLineSeparator).bytes)

        // Select 'kotlin'
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(dslPrompt)
        }
        handle.stdinPipe.write(("1" + TextUtil.platformLineSeparator).bytes)

        // Select default project name
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(projectNamePrompt)
        }
        handle.stdinPipe.write(TextUtil.platformLineSeparator.bytes)

        // Select 'no' for incubating APIs
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(incubatingPrompt)
        }
        handle.stdinPipe.write(("no" + TextUtil.platformLineSeparator).bytes)

        // after generating the project, we suggest the user reads some documentation
        ConcurrentTestUtil.poll(60) {
            assert handle.standardOutput.contains(documentationRegistry.getSampleForMessage())
        }
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        ScriptDslFixture.of(BuildInitDsl.KOTLIN, targetDir, null).assertGradleFilesGenerated()
    }

    @Issue("https://github.com/gradle/gradle/issues/26598")
    def "user can provide all necessary options to generate java application non-interactively"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks(
            "init",
            "--type", "java-application",
            "--dsl", "groovy",
            "--test-framework", "junit-jupiter",
            "--package", "my.project",
            "--project-name", "my-project",
            "--no-incubating",
            "--no-split-project",
            "--java-version", "14"
        )
        def handle = executer.start()
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        ScriptDslFixture.of(BuildInitDsl.GROOVY, targetDir, null).assertGradleFilesGenerated("app")
    }

    def "user can use defaults and provide no options to generate a basic project non-interactively"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks(
            "init",
            "--use-defaults",
        )
        def handle = executer.start()
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        ScriptDslFixture.of(BuildInitDsl.KOTLIN, targetDir, null).assertGradleFilesGenerated()
    }

    def "user can use defaults to generate java application non-interactively"() {
        when:
        executer.withForceInteractive(true)
        executer.withStdinPipe()
        executer.withTasks(
            "init",
            "--use-defaults",
            "--type", "java-application",
        )
        def handle = executer.start()
        handle.stdinPipe.close()
        handle.waitForFinish()

        then:
        ScriptDslFixture.of(BuildInitDsl.KOTLIN, targetDir, null).assertGradleFilesGenerated("app")
    }
}
