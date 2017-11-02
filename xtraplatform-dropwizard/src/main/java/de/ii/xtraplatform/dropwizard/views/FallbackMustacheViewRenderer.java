/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.views;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheResolver;
import com.github.mustachejava.resolver.FileSystemResolver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderException;
import io.dropwizard.views.mustache.MustacheViewRenderer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public class FallbackMustacheViewRenderer extends MustacheViewRenderer {

    private final LoadingCache<Class<? extends View>, MustacheFactory> factories;
    private boolean useCache = true;
    private Optional<File> fileRoot = Optional.empty();

    public FallbackMustacheViewRenderer() {
        this.factories = CacheBuilder.newBuilder().build(new CacheLoader<Class<? extends View>, MustacheFactory>() {
            @Override
            public MustacheFactory load(Class<? extends View> key) throws Exception {
                return createNewMustacheFactory(key);
            }
        });
    }

    @Override
    public boolean isRenderable(View view) {
        return view.getTemplateName().endsWith(getSuffix());
    }

    @Override
    public void render(View view, Locale locale, OutputStream output) throws IOException {
        try {
            final Charset charset = view.getCharset().orElse(StandardCharsets.UTF_8);
            try (OutputStreamWriter writer = new OutputStreamWriter(output, charset)) {
                render(view, writer);
            }
        } catch (Throwable e) {
            throw new ViewRenderException("Mustache template error: " + view.getTemplateName(), e);
        }
    }

    public void render(View view, OutputStreamWriter writer) throws IOException {
        try {
            final MustacheFactory mustacheFactory = useCache ? factories.get(view.getClass())
                    : createNewMustacheFactory(view.getClass());
            final Mustache template = mustacheFactory.compile(view.getTemplateName());

            template.execute(writer, view);
        } catch (Throwable e) {
            throw new ViewRenderException("Mustache template error: " + view.getTemplateName(), e);
        }
    }

    @Override
    public void configure(Map<String, String> options) {
        useCache = Optional.ofNullable(options.get("cache")).map(Boolean::parseBoolean).orElse(true);
        fileRoot = Optional.ofNullable(options.get("fileRoot")).map(File::new);
    }

    @VisibleForTesting
    boolean isUseCache() {
        return useCache;
    }

    @Override
    public String getSuffix() {
        return ".mustache";
    }

    private MustacheFactory createNewMustacheFactory(Class<? extends View> key) {
        return new DefaultMustacheFactory(
                fileRoot.isPresent() && fileRoot.get().exists() ? new FallbackMustacheResolver(new FileSystemResolver(fileRoot.get()), new PerClassMustacheResolver(key)) : new PerClassMustacheResolver(key));
    }

    class PerClassMustacheResolver implements MustacheResolver {
        private final Class<? extends View> klass;

        PerClassMustacheResolver(Class<? extends View> klass) {
            this.klass = klass;
        }

        @Override
        public Reader getReader(String resourceName) {
            final InputStream is = klass.getResourceAsStream(resourceName);
            if (is == null) {
                return null;
            }
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        }
    }
}
