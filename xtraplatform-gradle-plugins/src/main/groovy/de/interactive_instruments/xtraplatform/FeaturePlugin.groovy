package de.interactive_instruments.xtraplatform


import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

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

        project.configurations.default.extendsFrom(project.configurations.feature)
        project.configurations.default.extendsFrom(project.configurations.bundle)
        project.configurations.default.setTransitive(false)
        project.configurations.bundle.setTransitive(false)
        project.configurations.feature.setTransitive(true)

        /*project.extensions.javaPlatform.with {
            allowDependencies()
        }*/

        project.repositories {
            jcenter()
            maven {
                url "https://dl.bintray.com/iide/maven"
            }
        }

        addPublication(project)

        configureSubprojects(project)

        project.plugins.apply(DocPlugin.class)
    }

    void configureSubprojects(Project project) {
        project.subprojects { Project subproject ->

            subproject.plugins.apply('java-library')
            subproject.plugins.apply('maven-publish')
            subproject.plugins.apply(BundlePlugin.class)

            // stay java 8 compatible
            subproject.setSourceCompatibility(JavaVersion.VERSION_1_8)

            subproject.repositories {
                jcenter()
                maven {
                    url "https://dl.bintray.com/iide/maven"
                }
            }

            project.afterEvaluate {
                // add every feature as enforcedPlatform to provided
                // this allows to add bundles as dependencies without version
                project.configurations.feature.dependencies.each {
                    //println 'enforcedPlatform: ' + it
                    //TODO: does this work as intended?
                    //TODO: this adds all transitive dependencies, i think we have to split bom and bundles
                    subproject.dependencies.add('provided', subproject.dependencies.enforcedPlatform(it))
                }

                // add all bundles from xtraplatform-base with all transitive dependencies to compileOnly
                project.configurations.feature.resolvedConfiguration.firstLevelModuleDependencies.each({
                    if (it.moduleName == 'xtraplatform-base') {
                        it.children.each { bundle ->
                            //TODO
                            if (bundle.moduleGroup == 'de.interactive_instruments' || bundle.moduleName == 'org.apache.felix.ipojo') {
                                subproject.dependencies.add('compileOnly', bundle.name)
                                subproject.dependencies.add('testImplementation', bundle.name)
                            }
                        }
                    }
                })
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
                /*bom(MavenPublication) {
                    //from project.components.javaPlatform

                    artifactId "${project.name}-bom"

                    pom.withXml {

                        def dependencyManagementNode = asNode().appendNode('dependencyManagement').appendNode('dependencies')

                        project.configurations.bundle.dependencies.each {
                            def dependencyNode = dependencyManagementNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'compile')
                        }
                        //println asString()
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

                            /*def exclusionsNode = dependencyNode.appendNode('exclusions')
                            def exclusionNode = exclusionsNode.appendNode('exclusion')
                            exclusionNode.appendNode('groupId', '*')
                            exclusionNode.appendNode('artifactId', '*')*/
                        }

                        project.configurations.bundle.dependencies.each {
                            def dependencyNode = dependenciesNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'runtime')

                            /*def exclusionsNode = dependencyNode.appendNode('exclusions')
                            def exclusionNode = exclusionsNode.appendNode('exclusion')
                            exclusionNode.appendNode('groupId', '*')
                            exclusionNode.appendNode('artifactId', '*')*/
                        }

                        //asNode().appendNode('properties').appendNode('startLevel', '1')

                        //println asString()
                    }

                }
            }
        }
    }
}
