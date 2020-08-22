package de.ii.xtraplatform.dropwizard.app;

import de.ii.xtraplatform.dropwizard.domain.PartialMustacheResolver;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class PerClassMustacheResolver implements PartialMustacheResolver {

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public boolean canResolve(String templateName, Class<?> viewClass) {
        try {
            URL resource = viewClass.getResource(templateName);

            return Objects.nonNull(resource);

        } catch (Throwable e) {
            // ignore
        }
        return false;
    }

    @Override
    public Reader getReader(String templateName, Class<?> viewClass) {
        final InputStream is = viewClass.getResourceAsStream(templateName);
        if (is == null) {
            return null;
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }
}
