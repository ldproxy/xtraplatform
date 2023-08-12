/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.github.mustachejava.MustacheResolver;
import de.ii.xtraplatform.web.domain.MustacheResolverRegistry;
import de.ii.xtraplatform.web.domain.PartialMustacheResolver;
import io.dropwizard.views.common.View;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class MustacheResolverRegistryDefault implements MustacheResolverRegistry {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MustacheResolverRegistryDefault.class);

  private final Set<PartialMustacheResolver> partialMustacheResolvers;

  @Inject
  public MustacheResolverRegistryDefault(Set<PartialMustacheResolver> partialMustacheResolvers) {
    this.partialMustacheResolvers = partialMustacheResolvers;
  }

  @Override
  public MustacheResolver getResolverForClass(Class<? extends View> viewClass) {
    return templateName -> {
      Optional<PartialMustacheResolver> mustacheResolver =
          partialMustacheResolvers.stream()
              .sorted(Comparator.comparing(PartialMustacheResolver::getSortPriority).reversed())
              .filter(
                  partialMustacheResolver ->
                      partialMustacheResolver.canResolve(templateName, viewClass))
              .findFirst();

      return mustacheResolver
          .map(
              partialMustacheResolver -> partialMustacheResolver.getReader(templateName, viewClass))
          .orElse(null);
    };
  }
}
