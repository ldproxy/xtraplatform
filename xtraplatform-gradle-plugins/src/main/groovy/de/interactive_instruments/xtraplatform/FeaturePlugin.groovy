package de.interactive_instruments.xtraplatform


import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.slf4j.LoggerFactory

/**
 * @author zahnen
 */
class FeaturePlugin implements Plugin<Project> {

    static def LOGGER = LoggerFactory.getLogger(FeaturePlugin.class)

    @Override
    void apply(Project project) {
        //project.plugins.apply("java-platform")
        project.plugins.apply("java") // needed for platform constraints
        project.plugins.apply("maven-publish")

        project.configurations.create("feature")
        project.configurations.create("bundle")
        //project.configurations.create("default")

        //project.configurations.default.extendsFrom(project.configurations.feature)
        project.configurations.runtimeElements.extendsFrom(project.configurations.bundle)
        project.configurations.runtimeElements.setTransitive(false)
        project.configurations.bundle.setTransitive(false)
        project.configurations.feature.setTransitive(true)
        project.configurations.feature.resolutionStrategy.cacheDynamicVersionsFor(5, 'minutes')

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
            if (subproject.name == 'xtraplatform-runtime') {
                subproject.plugins.apply(RuntimePlugin.class)
            } else {
                subproject.plugins.apply(BundlePlugin.class)
            }

            // stay java 8 compatible
            subproject.setSourceCompatibility(JavaVersion.VERSION_1_8)

            subproject.repositories {
                jcenter()
                maven {
                    url "https://dl.bintray.com/iide/maven"
                }
            }


            subproject.afterEvaluate {
                if (subproject.version != null && subproject.version  != 'unspecified') {
                    LOGGER.warn("Warning: Bundle version '{}' is set for '{}'. Bundle versions are ignored, the feature version '{}' from '{}' is used instead.", subproject.version, subproject.name, project.version, project.name)
                    subproject.version = project.version
                }
            }

            def isIncludedBuild = (project.gradle.parent != null)

            //println subproject.name
            //println isIncludedBuild ? 'COMPOSITE' : 'STANDALONE'

            project.configurations.feature.incoming.beforeResolve {
                project.configurations.feature.dependencies.collect().each {
                    if (!it.name.endsWith("-bundles")) {
                        def bom = [group: it.group, name: "${it.name}", version: it.version]
                        def bundles = [group: it.group, name: "${it.name}-bundles", version: it.version]
                        if (isIncludedBuild) {

                        } else {
                            println 'platform: ' + bom
                            subproject.configurations.provided.incoming.afterResolve {
                                println "resolved provided for ${subproject.name}"

                                subproject.configurations.provided.incoming.dependencies.each({
                                    println it
                                    println it.attributes
                                    it.artifacts.each {a -> println "${a.name} ${a.type} ${a.extension} ${a.url} "}
                                })
                            }
                            subproject.dependencies.add('provided', subproject.dependencies.enforcedPlatform(bom))

                            println "added platform for ${subproject.name}"

                            project.dependencies.add('feature', bundles)
                        }
                    }
                }
            }

            project.afterEvaluate {

                // add all bundles from xtraplatform-base with all transitive dependencies to compileOnly
                project.configurations.feature.resolvedConfiguration.firstLevelModuleDependencies.each({
                    if (it.moduleName == 'xtraplatform-base' || it.moduleName == 'xtraplatform-base-bundles') {
                    	it.children.each { bundle ->
                            //TODO
                            //if (bundle.moduleGroup == 'de.interactive_instruments' || bundle.moduleName.startsWith("org.apache.felix.ipojo")) {
                                subproject.dependencies.add('compileOnly', bundle.name)
                                subproject.dependencies.add('testImplementation', bundle.name)
                                //subproject.dependencies.add('implementation', bundle.name)
                            //}
                        }
                    }
                })

                if (project.name == 'xtraplatform-base' && subproject.name != 'xtraplatform-runtime') {

                    def runtime = project.subprojects.find {it.name == 'xtraplatform-runtime'}
                    //println 'RUNTIME ' + runtime + ' ' + subproject

                    subproject.dependencies.add('compileOnly', runtime)
                    subproject.dependencies.add('testImplementation', runtime)
                    //subproject.dependencies.add('implementation', runtime)


                    // add all bundles from xtraplatform-base with all transitive dependencies to compileOnly
                    project.configurations.bundle.resolvedConfiguration.firstLevelModuleDependencies.each({ bundle ->
                                if (bundle.moduleName.startsWith('org.apache.felix.ipojo')) {
                                    subproject.dependencies.add('compileOnly', bundle.name)
                                    subproject.dependencies.add('testImplementation', bundle.name)
                                    //subproject.dependencies.add('implementation', bundle.name)
                                }
                    })
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
                    }
                }
            }
        }
    }

    void addPublication(Project project) {
        project.extensions.publishing.with {
            publications {
                'default'(MavenPublication) {

                    pom.withXml {

                        def dependencyManagementNode = asNode().appendNode('dependencyManagement').appendNode('dependencies')

                        project.configurations.bundle.dependencies.each {
                            def dependencyNode = dependencyManagementNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            //dependencyNode.appendNode('scope', 'compile')
                        }

                    }
                }
                bundles(MavenPublication) {

                    artifactId "${project.name}-bundles"

                    pom.withXml {

                        def dependenciesNode = asNode().appendNode('dependencies')

                        /*project.configurations.feature.dependencies.each {
                            def dependencyNode = dependenciesNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'runtime')
                        }*/

                        project.configurations.bundle.dependencies.each {
                            def dependencyNode = dependenciesNode.appendNode('dependency')
                            dependencyNode.appendNode('groupId', it.group)
                            dependencyNode.appendNode('artifactId', it.name)
                            dependencyNode.appendNode('version', it.version)
                            dependencyNode.appendNode('scope', 'runtime')
                        }

                    }

                }
            }
        }
    }
}
