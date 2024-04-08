/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.google.common.io.Resources;
import de.ii.xtraplatform.web.app.ResourceURL;
import java.net.URL;
import java.util.Optional;

public class StaticResourceReaderJar implements StaticResourceReader {
  private final Class<?> contextClass;

  public StaticResourceReaderJar(Class<?> contextClass) {
    this.contextClass = contextClass;
  }

  @Override
  public Optional<CachedResource> load(String path, Optional<String> defaultPage) {
    URL requestedResourceURL = null;
    byte[] requestedResourceBytes = null;
    // Try to determine whether we're given a resource with an actual file, or that
    // it is pointing to an (internal) directory. In the latter case, use the default
    // pages to search instead...
    try {
      requestedResourceURL = Resources.getResource(contextClass, path);
      requestedResourceBytes = Resources.toByteArray(requestedResourceURL);
      if (requestedResourceBytes.length == 0) {
        throw new IllegalStateException();
      }
    } catch (Throwable e) {
      // Given resource was a directory, stop looking for the actual resource
      // and check whether we can display a default page instead...
      if (defaultPage.isPresent()) {
        try {
          requestedResourceURL =
              Resources.getResource(contextClass, path + '/' + defaultPage.get());
          requestedResourceBytes = Resources.toByteArray(requestedResourceURL);
        } catch (Throwable e1) {
          // ignore
        }
      }
    }

    if (requestedResourceURL == null) {
      return Optional.empty();
    }

    long lastModified = ResourceURL.getLastModified(requestedResourceURL);

    return Optional.of(CachedResource.of(requestedResourceBytes, lastModified));
  }
}
