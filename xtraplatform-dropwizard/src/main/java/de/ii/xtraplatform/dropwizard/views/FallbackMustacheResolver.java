package de.ii.xtraplatform.dropwizard.views;

import com.github.mustachejava.MustacheResolver;

import java.io.Reader;

/**
 * @author zahnen
 */
public class FallbackMustacheResolver implements MustacheResolver {

    private MustacheResolver mustacheResolver;
    private MustacheResolver fallbackMustacheResolver;

    public FallbackMustacheResolver(MustacheResolver mustacheResolver, MustacheResolver fallbackMustacheResolver) {
        this.mustacheResolver = mustacheResolver;
        this.fallbackMustacheResolver = fallbackMustacheResolver;
    }

    @Override
    public Reader getReader(String resourceName) {
        Reader reader = mustacheResolver.getReader(resourceName);

        if (reader == null) {
            reader = fallbackMustacheResolver.getReader(resourceName);
        }

        return reader;
    }
}
