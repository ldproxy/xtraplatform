/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.github.mustachejava.resolver.FileSystemResolver;
import de.ii.xtraplatform.web.domain.PartialMustacheResolver;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class FileSystemMustacheResolver extends FileSystemResolver
    implements PartialMustacheResolver {

  private static final String TEMPLATE_DIR_NAME = "templates";
  private static final String HTML_DIR_NAME = "html";
  private static final String TEMPLATE_DIR_START = String.format("/%s/", TEMPLATE_DIR_NAME);
  private static final String TEMPLATE_HTML_DIR_START =
      String.format("/%s/%s/", TEMPLATE_DIR_NAME, HTML_DIR_NAME);

  private final Path templateDir;

  // TODO: via Xtraplatform?
  @Inject
  public FileSystemMustacheResolver() {
    super(Path.of("").toAbsolutePath().toFile());
    // super(Paths.get(bundleContext.getProperty(DATA_DIR_KEY)).toAbsolutePath().toFile());
    this.templateDir = Path.of("");
    // Paths.get(bundleContext.getProperty(DATA_DIR_KEY), TEMPLATE_DIR_NAME, HTML_DIR_NAME)
    //  .toAbsolutePath();
  }

  @Override
  public int getSortPriority() {
    return 1000;
  }

  @Override
  public boolean canResolve(String templateName, Class<?> viewClass) {
    Path templatePath;
    if (templateName.startsWith(TEMPLATE_DIR_START)) {
      templatePath =
          templateDir.resolve(templateName.substring(TEMPLATE_DIR_START.length())).toAbsolutePath();
    } else if (templateName.startsWith("/")) {
      templatePath = templateDir.resolve(templateName.substring(1)).toAbsolutePath();
    } else {
      templatePath = templateDir.resolve(templateName).toAbsolutePath();
    }

    return Files.isReadable(templatePath);
  }

  @Override
  public Reader getReader(String templateName, Class<?> viewClass) {
    return getReader(templateName.replace(TEMPLATE_DIR_START, TEMPLATE_HTML_DIR_START));
  }
}
