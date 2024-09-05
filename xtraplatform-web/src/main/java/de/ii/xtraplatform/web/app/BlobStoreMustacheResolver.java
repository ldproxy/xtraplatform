/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.web.domain.PartialMustacheResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class BlobStoreMustacheResolver implements PartialMustacheResolver, AppLifeCycle {

  private static final String TEMPLATE_DIR_NAME = "templates";
  private static final String HTML_DIR_NAME = "html";
  private static final String TEMPLATE_DIR_START = String.format("/%s/", TEMPLATE_DIR_NAME);

  private final ResourceStore templateStore;
  private final Map<Path, Optional<Path>> localPaths;

  @Inject
  public BlobStoreMustacheResolver(ResourceStore blobStore) {
    this.templateStore = blobStore.with(HTML_DIR_NAME, TEMPLATE_DIR_NAME);
    this.localPaths = new HashMap<>();
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    templateStore.onReady().thenRunAsync(this::init, Executors.newSingleThreadExecutor()).join();

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public int getSortPriority() {
    return 1000;
  }

  @Override
  public boolean canResolve(String templateName, Class<?> viewClass) {
    Path template = toPath(templateName);

    return localPaths.containsKey(template) && localPaths.get(template).isPresent();
  }

  @Override
  public Reader getReader(String templateName, Class<?> viewClass) {
    Optional<Path> localPath = localPaths.get(toPath(templateName));

    if (localPath.isPresent()) {
      try (InputStream inputStream =
          new ByteArrayInputStream(Files.readAllBytes(localPath.get()))) {

        return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      } catch (IOException e) {
        // continue
      }
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

  private void init() {
    try (Stream<Path> paths =
        templateStore.walk(Path.of(""), 1, (path, pathAttributes) -> pathAttributes.isValue())) {
      for (Iterator<Path> it = paths.iterator(); it.hasNext(); ) {
        Path path = it.next();

        try {
          Optional<Path> localPath = templateStore.asLocalPath(path, false);

          localPaths.put(path, localPath);
        } catch (IOException e) {
          // ignore
        }
      }
    } catch (IOException e) {
      // ignore
    }
  }
}
