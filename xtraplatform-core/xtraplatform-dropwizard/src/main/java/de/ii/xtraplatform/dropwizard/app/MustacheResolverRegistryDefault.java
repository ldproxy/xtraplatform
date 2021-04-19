/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.app;

import com.github.mustachejava.MustacheResolver;
import de.ii.xtraplatform.dropwizard.domain.MustacheResolverRegistry;
import de.ii.xtraplatform.dropwizard.domain.PartialMustacheResolver;
import de.ii.xtraplatform.runtime.domain.LogContext.MARKER;
import io.dropwizard.views.View;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
@Wbp(
    filter = "(objectClass=de.ii.xtraplatform.dropwizard.domain.PartialMustacheResolver)",
    onArrival = "onArrival",
    onDeparture = "onDeparture")
public class MustacheResolverRegistryDefault implements MustacheResolverRegistry {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MustacheResolverRegistryDefault.class);

  private final BundleContext bundleContext;
  private final List<PartialMustacheResolver> partialMustacheResolvers;

  public MustacheResolverRegistryDefault(@Context BundleContext bundleContext) {
    this.bundleContext = bundleContext;
    this.partialMustacheResolvers = new ArrayList<>();
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

  private synchronized void onArrival(ServiceReference<PartialMustacheResolver> ref) {
    final PartialMustacheResolver extension = bundleContext.getService(ref);
    int priority = extension.getSortPriority();

    if (LOGGER.isDebugEnabled(MARKER.DI)) {
      LOGGER.debug(
          MARKER.DI,
          "Registered partial mustache resolver with priority {}: {}",
          priority,
          extension);
    }

    partialMustacheResolvers.add(extension);
  }

  private synchronized void onDeparture(ServiceReference<PartialMustacheResolver> ref) {
    final PartialMustacheResolver extension = bundleContext.getService(ref);

    if (Objects.nonNull(extension)) {
      partialMustacheResolvers.remove(extension);
    }
  }
}
