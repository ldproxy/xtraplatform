/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.store.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.web.domain.PartialMustacheResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO: move back to xtraplatform-web, introduce xtraplatform-ops to remove store dependency on web
@Singleton
@AutoBind
public class BlobStoreMustacheResolver implements PartialMustacheResolver {

  private static final String TEMPLATE_DIR_NAME = "templates";
  private static final String HTML_DIR_NAME = "html";
  private static final String TEMPLATE_DIR_START = String.format("/%s/", TEMPLATE_DIR_NAME);

  private final BlobStore templateStore;

  @Inject
  public BlobStoreMustacheResolver(BlobStore blobStore) {
    this.templateStore = blobStore.with(HTML_DIR_NAME, TEMPLATE_DIR_NAME);
  }

  @Override
  public int getSortPriority() {
    return 1000;
  }

  @Override
  public boolean canResolve(String templateName, Class<?> viewClass) {
    try {
      return templateStore.has(toPath(templateName));
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public Reader getReader(String templateName, Class<?> viewClass) {
    try {
      Optional<InputStream> inputStream = templateStore.get(toPath(templateName));
      if (inputStream.isPresent()) {
        return new BufferedReader(new InputStreamReader(inputStream.get(), StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      // continue
    }
    return null;
  }

  private Path toPath(String templateName) {
    if (templateName.startsWith(TEMPLATE_DIR_START)) {
      return Path.of(templateName.substring(TEMPLATE_DIR_START.length()));
    } else if (templateName.startsWith("/")) {
      return Path.of(templateName.substring(1));
    }
    return Path.of(templateName);
  }
}
