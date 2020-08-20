package de.ii.xtraplatform.dropwizard.views;

import com.github.mustachejava.resolver.FileSystemResolver;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

@Component
@Provides
@Instantiate
public class FileSystemMustacheResolver extends FileSystemResolver implements PartialMustacheResolver {

    private static final String TEMPLATE_DIR_NAME = "templates";

    private final Path templateDir;

    public FileSystemMustacheResolver(@Context BundleContext bundleContext) {
        super(Paths.get(bundleContext.getProperty(DATA_DIR_KEY))
                   .toAbsolutePath()
                   .toFile());
        this.templateDir = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), TEMPLATE_DIR_NAME)
                                .toAbsolutePath();
    }

    @Override
    public int getSortPriority() {
        return 1000;
    }

    @Override
    public boolean canResolve(String templateName, Class<?> viewClass) {
        Path templatePath;
        if (templateName.startsWith("/" + TEMPLATE_DIR_NAME + "/")) {
            templatePath = templateDir.resolve(templateName.substring(TEMPLATE_DIR_NAME.length()+2)).toAbsolutePath();
        } else if (templateName.startsWith("/")) {
            templatePath = templateDir.resolve(templateName.substring(1)).toAbsolutePath();
        } else {
            templatePath = templateDir.resolve(templateName).toAbsolutePath();
        }

        return Files.isReadable(templatePath);
    }

    @Override
    public Reader getReader(String templateName, Class<?> viewClass) {
        return getReader(templateName);
    }
}
