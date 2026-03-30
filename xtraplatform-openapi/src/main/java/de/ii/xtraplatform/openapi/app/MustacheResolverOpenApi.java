/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.openapi.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.web.domain.PartialMustacheResolver;
import de.ii.xtraplatform.web.domain.PerClassMustacheResolver;
import java.io.Reader;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Reuse the per class loader, but switch to the OpenAPI module context. */
@Singleton
@AutoBind
public class MustacheResolverOpenApi extends PerClassMustacheResolver
    implements PartialMustacheResolver {

  @Inject
  MustacheResolverOpenApi() {
    super();
  }

  @Override
  public int getSortPriority() {
    return 200;
  }

  @Override
  public boolean canResolve(String templateName, Class<?> viewClass) {
    return super.canResolve(templateName, this.getClass());
  }

  @Override
  public Reader getReader(String templateName, Class<?> viewClass) {
    return super.getReader(templateName, this.getClass());
  }
}
