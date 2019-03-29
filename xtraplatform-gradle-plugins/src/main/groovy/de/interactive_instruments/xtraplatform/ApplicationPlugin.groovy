package de.interactive_instruments.xtraplatform

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.internal.impldep.org.bouncycastle.math.raw.Mod

import java.util.logging.FileHandler
import java.util.regex.Pattern

/**
 * @author zahnen
 */
class ApplicationPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(FeaturePlugin.class)
        project.plugins.apply("application")

        project.configurations.create("featureDevOnly")
        project.getConfigurations().create("app")
        //project.configurations.app.setTransitive(false)
        project.configurations.implementation.extendsFrom(project.configurations.app)
        //project.getConfigurations().create("platform")
        //project.configurations.platform.setTransitive(false)
        //project.configurations.compileOnly.extendsFrom(project.configurations.platform)

        project.afterEvaluate {
            def baseFound = false
            project.configurations.feature.dependencies.each {
                if (it.name == 'xtraplatform-base') {
                    project.dependencies.add('app', 'de.interactive_instruments:xtraplatform-runtime')
                    baseFound = true
                }
            }
            if (!baseFound) {
                throw new IllegalStateException("You have to add 'xtraplatform-base' to configuration 'feature'")
            }
        }

        project.repositories {
            jcenter()
            maven {
                url "https://dl.bintray.com/iide/maven"
            }
        }

        addCreateRuntimeClassTask(project)

        addDistribution(project)

        addRunConfiguration(project)
    }

    void addRunConfiguration(Project project) {
        def dataDir = new File(project.buildDir, 'data')

        project.task('initData') {
            doLast {
                dataDir.mkdirs()
            }
        }

        project.task('cleanFelixCache', type: Delete) {
            delete new File(dataDir, 'felix-cache')
        }

        project.task('addDevBundles', type: Copy) {
            dependsOn project.tasks.installDist
            into project.tasks.installDist.destinationDir
            project.afterEvaluate {
                from(getDevBundleFiles(project)) {
                    into "bundles"
                }
            }
        }

        project.tasks.run.with {
            dependsOn project.tasks.installDist
            dependsOn project.tasks.addDevBundles
            dependsOn project.tasks.initData
            dependsOn project.tasks.cleanFelixCache
            workingDir = project.tasks.installDist.destinationDir
            args dataDir.absolutePath
            standardInput = System.in
            environment 'XTRAPLATFORM_ENV', 'DEVELOPMENT'
            //suppress java 9+ illegal access warnings for felix and jackson afterburner
            if (JavaVersion.current().isJava9Compatible()) {
                jvmArgs '--add-opens', 'java.base/java.lang=ALL-UNNAMED', '--add-opens', 'java.base/java.net=ALL-UNNAMED', '--add-opens', 'java.base/java.security=ALL-UNNAMED'
            }
        }
    }

    void addDistribution(Project project) {
        project.afterEvaluate {
            project.distributions.with {
                main {
                    contents {
                        from(getBundleFiles(project)) {
                            into "bundles"
                        }
                        into('') {
                            //create an empty 'data/log' directory in distribution root
                            def appDirBase = new File(project.buildDir, 'tmp/app-dummy-dir')
                            def logDir = new File(appDirBase, 'data/log')
                            logDir.mkdirs()

                            from { appDirBase }
                        }
                    }
                }
            }
        }

        //project.tasks.startScripts.unixStartScriptGenerator.template = project.resources.text.fromFile('gradle/sh-start-script')

        // for docker
        project.tasks.distTar.version = ''
    }

    List<File> getBundleFiles(Project project) {
        def bundlesFromFeatures = project.configurations.feature.resolvedConfiguration.firstLevelModuleDependencies.collectMany({
            it.children.collectMany({ it.moduleArtifacts }).findAll({ it.name != 'xtraplatform-runtime' }).collect({
                it.file
            })
        })
        def bundlesFromApplication = project.configurations.bundle.resolvedConfiguration.firstLevelModuleDependencies.collectMany({
            it.moduleArtifacts
        }).collect({ it.file })

        return bundlesFromFeatures + bundlesFromApplication
    }

    List<File> getDevBundleFiles(Project project) {
        return project.configurations.featureDevOnly.resolvedConfiguration.firstLevelModuleDependencies.collectMany({
            it.children.collectMany({ it.moduleArtifacts }).collect({
                it.file
            })
        })
    }

    void addCreateRuntimeClassTask(Project project) {
        project.mainClassName = "de.ii.xtraplatform.application.Launcher"

        File generatedSourceDir = new File(project.buildDir, 'generated/src/main/java/')
        project.mkdir(generatedSourceDir)

        project.sourceSets.main.java { project.sourceSets.main.java.srcDir generatedSourceDir }

        project.task('createRuntimeClass') {
            inputs.files project.configurations.feature
            inputs.files project.configurations.featureDevOnly
            inputs.files project.configurations.bundle
            outputs.dir(generatedSourceDir)

            doLast {

                def bundles = createBundleTree(project)
                def devBundles = createDevBundleTree(project)

                def mainClass = """
                    package de.ii.xtraplatform.application;

                    import de.ii.xtraplatform.runtime.FelixRuntime;
                    import com.google.common.collect.ImmutableList;
                    import java.lang.Runtime;
                    import java.util.List;
        
                    public class Launcher {
                    
                        private static final List<List<String>> BUNDLES = ${bundles};
                        private static final List<List<String>> DEV_BUNDLES = ${devBundles};

                        public static void main(String[] args) throws Exception {
                            final FelixRuntime runtime = new FelixRuntime("${project.name}", "${project.version}");
                            
                            runtime.init(args, BUNDLES, DEV_BUNDLES);
                            runtime.start();
                            
                            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                runtime.stop(5000);
                            }));
                        }
                    }
                """

                File packageDir = new File(generatedSourceDir, 'de/ii/xtraplatform/application/')
                packageDir.mkdirs()

                //println packageDir
                //println mainClass

                new File(packageDir, "Launcher.java").write(mainClass)
            }
        }

        project.tasks.compileJava.with {
            inputs.dir(generatedSourceDir)
            dependsOn project.tasks.createRuntimeClass
        }

    }

    String createBundleTree(Project project) {
        def features = sortByDependencyGraph(project.configurations.feature.resolvedConfiguration.firstLevelModuleDependencies)
        def bundles = features.collect({ it.children.findAll({ bundle -> !(bundle in features) }) })

        bundles.add(project.configurations.bundle.resolvedConfiguration.firstLevelModuleDependencies)

        return createBundleFileTree(project, bundles, ['xtraplatform-runtime'], 'de.ii.xtraplatform.entity.api.handler:entity', ['xtraplatform-server'], ['xtraplatform-dropwizard', 'osgi-over-slf4j', 'org.apache.felix.ipojo'])
    }

    String createDevBundleTree(Project project) {
        def devFeatures = sortByDependencyGraph(project.configurations.featureDevOnly.resolvedConfiguration.firstLevelModuleDependencies)
        def devBundles = devFeatures.collect({ it.children.findAll({ bundle -> !(bundle in devFeatures) }) })

        return createBundleFileTree(project, devBundles)
    }

    Set<ResolvedDependency> sortByDependencyGraph(Set<ResolvedDependency> features) {
        return features.toSorted { featureA, featureB ->
            if (featureA == featureB) return 0
            def dependsOn = featureA.children.stream().anyMatch({ child -> child == featureB })
            return dependsOn ? 1 : -1
        }
    }

    String createBundleFileTree(Project project, List<Set<ResolvedDependency>> features, List<String> excludeNames = [], String lateStartManifestPattern = "", List<String> lateStartNames = [], List<String> earlyStartNames = []) {

        String bundleTree = 'ImmutableList.of('
        String featureBundles = ''
        List<File> delayedBundles = []
        List<File> lastBundles = []
        List<File> firstBundles = []

        features.eachWithIndex { feature, index ->

            def bundles = feature.collectMany({ bundle ->
                bundle.moduleArtifacts
            }).findAll({ !(it.name in excludeNames) }).collect({
                it.file
            })

            delayedBundles += bundles.findAll({ bundle -> manifestContains(project, bundle, lateStartManifestPattern) })
            lastBundles += bundles.findAll({ bundle -> lateStartNames.any({ bundle.name.startsWith(it) }) })
            firstBundles += bundles.findAll({ bundle -> earlyStartNames.any({ bundle.name.startsWith(it) }) })

            featureBundles += createBundleList(bundles.findAll({ bundle -> !(bundle in delayedBundles) && !(bundle in lastBundles) && !(bundle in firstBundles) }))

            if (index < features.size() - 1) {
                featureBundles += ','
            }
        }

        if (!firstBundles.isEmpty()) {
            bundleTree += createBundleList(firstBundles) + ','
        }
        bundleTree += featureBundles
        if (!delayedBundles.isEmpty()) {
            bundleTree += ','

            bundleTree += createBundleList(delayedBundles)
        }
        if (!lastBundles.isEmpty()) {
            bundleTree += ','

            bundleTree += createBundleList(lastBundles)
        }

        bundleTree += ')'
    }

    String createBundleList(List<File> bundles) {
        def bundleList = '\nImmutableList.of('

        bundles.eachWithIndex { bundle, index2 ->
            //println "- " + bundle.name

            bundleList += '"' + bundle.name + '"'

            if (index2 < bundles.size() - 1) {
                bundleList += ','
            }
        }

        bundleList += ')'
    }

    boolean manifestContains(Project project, File jar, String value) {
        if (value == null || value.isEmpty()) return false;

        Pattern pattern = stringToRegex(value)

        return project
                .zipTree(jar)
                .matching({ it.include('**/META-INF/MANIFEST.MF') })
                .files
                .any({ manifest -> pattern.matcher(manifest.text).find() })
    }

    Pattern stringToRegex(String value) {
        String pattern = ''
        value.each { ch ->
            pattern += ch.replaceAll("([^a-zA-Z0-9 ])", '\\\\$1') + '\\s*'
        }
        return Pattern.compile(pattern)
    }
}
