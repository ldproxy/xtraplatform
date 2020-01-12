package de.interactive_instruments.xtraplatform

import org.gradle.api.Plugin
import org.gradle.api.Project

class RuntimePlugin implements Plugin<Project> {

    static class SystemBundleExtension {
        Map<String,String> exports = [:]
    }

    @Override
    void apply(Project project) {

        project.configurations.create('systemBundle')

        project.configurations.api.extendsFrom(project.configurations.systemBundle)

        def extension = project.extensions.create('systemBundle', SystemBundleExtension)

        def packageName = 'de.ii.xtraplatform.runtime'
        def packagePath = 'de/ii/xtraplatform/runtime/'
        def className = 'Exports'

        ClassGenerator.generateClassTask(project, 'pkgs', packagePath, className, {}, {

            def exports = 'new ImmutableMap.Builder<String,String>()'

            def excludes = []

            // determine artifacts that should be included in the bundle, might be transitive or not
            def deps = Dependencies.getDependencies(project, 'systemBundle', excludes, true)

            // determine packages for export
            def pkgs = Dependencies.getPackages(deps)

            pkgs.each { pkg ->
                //println "${pkg.name} ${pkg.version} (${pkg.dep})"
                //project.jar.manifest.instruction("Export-Package", "${pkg.name};version=${pkg.version.replaceAll('(-[\\w]+)+$', '')}")
                //project.jar.manifest.instruction("Import-Package", "${pkg.name};version=${pkg.version.replaceAll('(-[\\w]+)+$', '')}")

                exports += "\n.put(\"${pkg.name}\", \"${pkg.version}\")"
            }

            extension.exports.each { pkg ->
                //println "${pkg.key} ${pkg.value}"

                exports += "\n.put(\"${pkg.key}\", \"${pkg.value}\")"
            }

            exports += "\n.put(\"${packageName}\", \"${project.version}\")"
            exports += "\n.put(\"de.ii.xtraplatform.configuration\", \"${project.version}\")"

            exports += "\n.build()"

            return """
                package ${packageName};

                import com.google.common.collect.ImmutableMap;
                import java.util.Map;
    
                class ${className} {
                
                    static final Map<String, String> EXPORTS = ${exports};

                }
            """

        })
    }
}
