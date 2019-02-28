package de.interactive_instruments.xtraplatform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.maven.MavenPublication

/**
 * @author zahnen
 */
class FeaturePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //project.plugins.apply("java-platform")
        project.plugins.apply("maven-publish")

        project.configurations.create("feature")
        project.configurations.create("bundle")
        project.configurations.create("default")
        //TODO: api or runtime
        project.configurations.default.extendsFrom(project.configurations.feature)
        project.configurations.default.extendsFrom(project.configurations.bundle)

        /*project.extensions.javaPlatform.with {
            allowDependencies()
        }*/

        addPublication(project)

        configureSubprojects(project)


        //TODO: enforcePlatform here or in build.gradle

        //TODO: apply bundles.gradle


    }

    void configureSubprojects(Project project) {
        project.subprojects { Project subproject ->

            subproject.plugins.apply('java-library')
            subproject.plugins.apply('maven-publish')
            //TODO: does it work?, move to bnd plugin
            subproject.plugins.apply('de.interactive_instruments.xtraplatform-osgi')

            subproject.repositories {
                jcenter()
                maven {
                    url "https://dl.bintray.com/iide/maven"
                }
            }

            project.afterEvaluate {
                project.configurations.feature.dependencies.each {
                    println 'enforcedPlatform: ' + it
                    //TODO: does this work as intended?
                    subproject.dependencies.add('implementation', subproject.dependencies.enforcedPlatform(it))
                }
            }

            subproject.extensions.publishing.with {
                publications {
                    'default'(MavenPublication) {
                        from subproject.components.java
                    }
                }
            }


        }
    }

    void addPublication(Project project) {
        project.extensions.publishing.with {
            publications {
                /*feature(MavenPublication) {
                from project.components.javaPlatform

                artifactId "${project.name}-bom"

                pom.withXml {

                    println asString()
                }
            }*/
                'default'(MavenPublication) {

                    //artifactId "${project.name}-bundles"

                    pom.withXml {

                        def dependencyManagementNode = asNode().appendNode('dependencyManagement').appendNode('dependencies')

                        project.configurations.bundle.dependencies.each {
                            def dependencyNode = dependencyManagementNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'compile')
                        }

                        def dependenciesNode = asNode().appendNode('dependencies')

                        project.configurations.feature.dependencies.each {
                            def dependencyNode = dependenciesNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'runtime')
                        }

                        project.configurations.bundle.dependencies.each {
                            def dependencyNode = dependenciesNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'runtime')
                        }

                        //asNode().appendNode('properties').appendNode('startLevel', '1')

                        //println asString()
                    }

                }
            }
        }
    }
}
