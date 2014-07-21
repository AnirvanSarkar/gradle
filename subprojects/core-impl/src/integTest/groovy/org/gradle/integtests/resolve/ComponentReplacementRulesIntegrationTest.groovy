/*
 * Copyright 2014 the original author or authors.
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




package org.gradle.integtests.resolve

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class ComponentReplacementRulesIntegrationTest extends AbstractIntegrationSpec {

    def "selects highest guava over google collections"() {
        buildFile << """
            repositories { mavenCentral() }
            configurations { foo }
            dependencies {
                foo "com.google.collections:google-collections:1.0"
                foo "com.google.guava:guava:16.0.1"
                foo "com.google.guava:guava:15.0.0"
                components {
                    replacements {
                        from('com.google.collections:google-collections').into('com.google.guava:guava')
                    }
                }
            }
            task check << {
                assert configurations.foo.files*.name == ['guava-16.0.1.jar']
            }
        """
        expect:
        run "dependencies", "check"
    }

    def "selects highest guava when google collections was already selected earlier"() {
        mavenRepo.module("foo", "bar").dependsOn("com.google.guava", "guava", "16.0.1").publish()
        mavenRepo.module("foo", "bar", "0.5").publish()
        buildFile << """
            repositories {
              mavenCentral()
              maven { url "$mavenRepo.uri" }
            }
            configurations { foo }
            dependencies {
                foo "com.google.collections:google-collections:1.0"
                foo "foo:bar:1.0"
                foo "foo:bar:0.5"
                components.replacements.from('com.google.collections:google-collections').into('com.google.guava:guava')
            }
            task check << {
                assert configurations.foo.files*.name == ['bar-1.0.jar', 'guava-16.0.1.jar']
            }
        """
        expect:
        run "dependencies", "check"
    }

    def "prefers google collections over guava"() {
        buildFile << """
            repositories { mavenCentral() }
            configurations { foo }
            dependencies {
                foo "com.google.collections:google-collections:1.0"
                foo "com.google.guava:guava:16.0.1"
                foo "com.google.guava:guava:15.0.0"
                components.replacements.from('com.google.guava:guava').into('com.google.collections:google-collections')
            }

            task check << {
                assert configurations.foo.files*.name == ['google-collections-1.0.jar']
            }
        """
        expect:
        run "dependencies", "check"
    }

    def "ignores replacement if only older version on classpath"() {
        buildFile << """
            repositories { mavenCentral() }
            configurations { foo }
            dependencies {
                foo "com.google.guava:guava:16.0.1"
                foo "com.google.guava:guava:15.0.0"
                components.replacements.from('com.google.guava:guava').into('com.google.collections:google-collections')
            }
            task check << {
                assert configurations.foo.files*.name == ['guava-16.0.1.jar']
            }
        """
        expect:
        run "check"
        run "dependencies"
    }

    def "prefers spring 3.x over 2.x"() {
        buildFile << """
            repositories { mavenCentral() }
            configurations { foo {
                    transitive = false //avoid spring transitive dependencies
                }
            }
            dependencies {
                foo "org.springframework:spring:2.5.6"
                foo "org.springframework:spring:2.5.5"
                foo "org.springframework:spring-core:3.2.5.RELEASE"
                foo "org.springframework:spring-core:3.2.6.RELEASE"
                foo "org.springframework:spring-instrument:3.2.4.RELEASE"
                components.replacements.from('org.springframework:spring').into {
                  it.group == 'org.springframework' && it.name.startsWith('spring-')
                }
            }

            task check << {
                assert configurations.foo.files*.name == ['spring-instrument-3.2.4.RELEASE.jar', 'spring-core-3.2.6.RELEASE.jar']
            }
        """

        expect:
        run "dependencies", "check"
    }

    def "prefers kafka_2.10"() {
        buildFile << """
            repositories { mavenCentral() }
            configurations { foo {
                    transitive = false //avoid spring transitive dependencies
                }
            }
            dependencies {
                foo "org.apache.kafka:kafka_2.10:0.8.0"
                foo "org.apache.kafka:kafka_2.9.1:0.8.1"
                foo "org.apache.kafka:kafka_2.9.1:0.8.0"
                foo "org.apache.kafka:kafka_2.8.0:0.8.0"
                components.replacements.from {
                  it.name.endsWith('_2.8.0') || it.name.endsWith('_2.9.1')
                }.into {
                  it.name.endsWith('_2.10')
                }
            }

            task check << {
                assert configurations.foo.files*.name == ['kafka_2.10-0.8.0.jar']
            }
        """
        expect:
        run "check"
        run "dependencies"
    }

    //TODO SF ensure we don't include the dependencies of replaced module in the graph
    //TODO SF ensure we the resolution result has the module correctly replaced
    //TODO SF ensure the selection reason is correct
    //TODO SF ensure we the resolved configuration has the module correctly replaced
    //TODO SF specify behavior for interfering dependency resolve rules and component replacements
}
