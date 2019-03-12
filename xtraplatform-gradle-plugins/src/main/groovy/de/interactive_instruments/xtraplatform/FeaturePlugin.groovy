package de.interactive_instruments.xtraplatform

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.impldep.com.google.common.base.Strings

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

        // needed for composite builds in intellij
        /*project.task('jar') {
            dependsOn(getSubprojectTasksByName(project, 'jar'))
        }
        project.task('classes') {
            dependsOn(getSubprojectTasksByName(project, 'classes'))
        }
        project.task('testClasses') {
            dependsOn(getSubprojectTasksByName(project, 'testClasses'))
        }*/


    }

    void configureSubprojects(Project project) {
        project.subprojects { Project subproject ->

            subproject.plugins.apply('java-library')
            subproject.plugins.apply('maven-publish')
            //TODO: does it work?, move to bnd plugin, version?
            subproject.plugins.apply(IpojoPlugin.class) //.apply('de.interactive_instruments.xtraplatform-bundle')

            subproject.repositories {
                jcenter()
                maven {
                    url "https://dl.bintray.com/iide/maven"
                }
            }

            project.afterEvaluate {
                project.configurations.feature.dependencies.each {
                    //println 'enforcedPlatform: ' + it
                    //TODO: does this work as intended?
                    //TODO: this adds all transitive dependencies, i think we have to split bom and bundles
                    subproject.dependencies.add('implementation', subproject.dependencies.enforcedPlatform(it))

                    //TODO: does it work?, only for base or for all features?
                    if (it.name == 'xtraplatform-base') {
                        //println 'base: ' + it
                        subproject.dependencies.add('implementation', it)
                    }
                }
            }

            subproject.task('sourceJar', type: Jar) {
                from sourceSets.main.allSource
            }

            subproject.extensions.publishing.with {
                publications {
                    'default'(MavenPublication) {
                        from subproject.components.java

                        artifact sourceJar {
                            classifier "sources"
                        }

                        /*pom.withXml{
                            asNode().remove(asNode().get('dependencies'))
                            def dependenciesNode = asNode().appendNode('dependencies')

                            subproject.configurations.provided.allDependencies.each {
                                def dependencyNode = dependenciesNode.appendNode('dependency')
                                dependencyNode.appendNode('groupId', it.group)
                                dependencyNode.appendNode('artifactId', it.name)
                                dependencyNode.appendNode('version', it.version)
                                dependencyNode.appendNode('scope', 'runtime')

                                def exclusionsNode = dependencyNode.appendNode('exclusions')
                                def exclusionNode = exclusionsNode.appendNode('exclusion')
                                exclusionNode.appendNode('groupId', '*')
                                exclusionNode.appendNode('artifactId', '*')
                            }
                        }*/
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

    Set<Task> getSubprojectTasksByName(Project project, String name) {
            final Set<Task> foundTasks = new HashSet();

            project.subprojects({ subproject ->
                ((ProjectInternal)subproject).evaluate();
                Task task = (Task)subproject.getTasks().findByName(name);
                if (task != null) {
                    foundTasks.add(task);
                }
            });

            return foundTasks;
    }
}
