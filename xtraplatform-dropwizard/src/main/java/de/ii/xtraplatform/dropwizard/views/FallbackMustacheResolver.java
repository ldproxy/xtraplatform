/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
