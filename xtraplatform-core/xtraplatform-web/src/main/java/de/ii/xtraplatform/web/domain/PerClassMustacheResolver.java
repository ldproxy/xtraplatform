/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PerClassMustacheResolver implements PartialMustacheResolver {

  @Inject
  public PerClassMustacheResolver() {}

  @Override
  public int getSortPriority() {
    return 0;
  }

  @Override
  public boolean canResolve(String templateName, Class<?> viewClass) {
    try {
      URL resource = Resources.getResource(viewClass, getQualifiedName(templateName, viewClass));

      return Objects.nonNull(resource);

    } catch (Throwable e) {
      // ignore
    }
    return false;
  }

  @Override
  public Reader getReader(String templateName, Class<?> viewClass) {
    final InputStream is;
    try {
      is =
          Resources.asByteSource(
                  Resources.getResource(viewClass, getQualifiedName(templateName, viewClass)))
              .openStream();
    } catch (IOException e) {
      return null;
    }
    return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
  }

  private String getQualifiedName(String templateName, Class<?> viewClass) {
    Module module = viewClass.getModule();
    String pkg = module.getName().replaceAll("\\.", "/");
    String tmpl = String.format("/%s%s", pkg, templateName);
    return tmpl;
  }
}
