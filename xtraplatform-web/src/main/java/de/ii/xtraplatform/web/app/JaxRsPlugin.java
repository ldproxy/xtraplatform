/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Sets;
import dagger.Lazy;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.web.domain.AuthProvider;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import de.ii.xtraplatform.web.domain.Endpoint;
import de.ii.xtraplatform.web.domain.JaxRsConsumer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.internal.inject.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class JaxRsPlugin implements DropwizardPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsPlugin.class);

  private final Lazy<Set<AuthProvider<?>>> authProviders;
  private final Lazy<Set<Endpoint>> endpoints;
  private final Lazy<Set<ContainerRequestFilter>> containerRequestFilters;
  private final Lazy<Set<ContainerResponseFilter>> containerResponseFilters;
  private final Lazy<Set<ExceptionMapper<?>>> exceptionMappers;
  private final Lazy<Set<Binder>> binders;
  private boolean isAuthProviderAvailable;
  private final Lazy<Set<JaxRsConsumer>> consumers;

  // TODO: DynamicFeature ???
  @Inject
  JaxRsPlugin(
      Lazy<Set<Endpoint>> endpoints,
      Lazy<Set<JaxRsConsumer>> consumers,
      Lazy<Set<AuthProvider<?>>> authProviders,
      Lazy<Set<ContainerRequestFilter>> containerRequestFilters,
      Lazy<Set<ContainerResponseFilter>> containerResponseFilters,
      Lazy<Set<ExceptionMapper<?>>> exceptionMappers,
      Lazy<Set<Binder>> binders
      // Lazy<Set<DynamicFeature>> dynamicFeatures
      ) {

    this.authProviders = authProviders;
    this.endpoints = endpoints;
    this.containerRequestFilters = containerRequestFilters;
    this.containerResponseFilters = containerResponseFilters;
    this.exceptionMappers = exceptionMappers;
    this.binders = binders;
    this.consumers = consumers;
    // TODO
    this.isAuthProviderAvailable = false;
  }

  // should be last
  @Override
  public int getPriority() {
    return 10_000;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    JerseyEnvironment jersey = environment.jersey();

    setupAuthProviders(jersey);
    registerEndpoints(jersey);
    registerFilters(jersey);
    registerBinders(jersey);
    registerExceptionMappers(jersey);
    processConsumers(jersey);
  }

  private void setupAuthProviders(JerseyEnvironment jersey) {
    for (AuthProvider<?> provider : authProviders.get()) {
      if (!isAuthProviderAvailable) {
        jersey.register(provider.getRolesAllowedDynamicFeature());
        jersey.register(provider.getAuthValueFactoryProvider());
        this.isAuthProviderAvailable = true;
      }
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(MARKER.DI, "Registered JAX-RS Auth Provider {}", provider.getClass());
      }
    }
    if (isAuthProviderAvailable) {
      List<AuthFilter> filters =
          authProviders.get().stream()
              .sorted(
                  Comparator.<AuthProvider<?>>comparingInt(AuthProvider::getPriority).reversed())
              .map(AuthProvider::getAuthFilter)
              .collect(Collectors.toList());
      jersey.register(
          new WebApplicationExceptionCatchingFilterPreMatching(new ChainedAuthFilter<>(filters)));
    }
  }

  private void registerEndpoints(JerseyEnvironment jersey) {
    if (isAuthProviderAvailable && !endpoints.get().isEmpty()) {
      for (Object resource : endpoints.get()) {
        jersey.register(resource);
        if (LOGGER.isDebugEnabled(MARKER.DI)) {
          LOGGER.debug(MARKER.DI, "Registered JAX-RS Resource {}", resource.getClass());
        }
      }
    } else if (!isAuthProviderAvailable
        && !endpoints.get().isEmpty()
        && LOGGER.isDebugEnabled(MARKER.DI)) {
      LOGGER.debug(MARKER.DI, "No JAX-RS Auth Provider registered yet, cannot register Resources.");
    }
  }

  private void registerFilters(JerseyEnvironment jersey) {
    for (ContainerRequestFilter filter : containerRequestFilters.get()) {
      jersey.register(filter);
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(MARKER.DI, "Registered JAX-RS ContainerRequestFilter {})", filter.getClass());
      }
    }
    for (ContainerResponseFilter filter : containerResponseFilters.get()) {
      jersey.register(filter);
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(MARKER.DI, "Registered JAX-RS ContainerResponseFilter {})", filter.getClass());
      }
    }
  }

  private void registerBinders(JerseyEnvironment jersey) {
    for (Binder binder : binders.get()) {
      jersey.register(binder);
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(MARKER.DI, "Registered JAX-RS Binder {}", binder.getClass());
      }
    }
  }

  private void registerExceptionMappers(JerseyEnvironment jersey) {
    for (ExceptionMapper<?> exceptionMapper : exceptionMappers.get()) {
      jersey.register(exceptionMapper);
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(MARKER.DI, "Registered JAX-RS ExceptionMapper {}", exceptionMapper.getClass());
      }
    }
  }

  private void processConsumers(JerseyEnvironment jersey) {
    for (JaxRsConsumer consumer : consumers.get()) {
      if (consumer != null) {
        consumer
            .getConsumer()
            .accept(
                Sets.union(
                    jersey.getResourceConfig().getInstances(),
                    jersey.getResourceConfig().getSingletons()));
      }
    }
  }
}
