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
import de.ii.xtraplatform.web.domain.JaxRsChangeListener;
import de.ii.xtraplatform.web.domain.JaxRsConsumer;
import de.ii.xtraplatform.web.domain.JaxRsReg;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.inject.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
// used basic idea from https://github.com/hstaudacher/osgi-jax-rs-connector
//
@Singleton
@AutoBind
public class JaxRsRegistry implements JaxRsReg, DropwizardPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsRegistry.class);
  public static final String PUBLISH = "de.ii.xsf.jaxrs.publish";
  public static final String ANY_SERVICE_FILTER = "(&(objectClass=*)(!(" + PUBLISH + "=false)))";

  // private final MutableServletContextHandler server;
  private final Lazy<Set<AuthProvider<?>>> authProviders;
  private final Lazy<Set<Endpoint>> endpoints;
  private final Lazy<Set<ContainerRequestFilter>> containerRequestFilters;
  private final Lazy<Set<ContainerResponseFilter>> containerResponseFilters;
  private final Lazy<Set<ExceptionMapper<?>>> exceptionMappers;
  private final Lazy<Set<Binder>> binders;
  private final List<Object> providerCache;
  private final List<Object> filterCache;
  private boolean isJerseyAvailable;
  private boolean isAuthProviderAvailable;
  // private final Dropwizard dw;
  // private ResourceConfig jersey;
  private final List<JaxRsChangeListener> changeListeners;
  private final Lazy<Set<JaxRsConsumer>> consumers;

  // TODO: DropwizardEnvironmentPlugin
  // TODO: any other @Provider besides Binder?
  // TODO: use marker interface to register Providers
  @Inject
  JaxRsRegistry(
      Lazy<Set<Endpoint>> endpoints,
      Lazy<Set<JaxRsConsumer>> consumers,
      // Lazy<Set<Binder>> binders,
      Lazy<Set<AuthProvider<?>>> authProviders,
      Lazy<Set<ContainerRequestFilter>> containerRequestFilters,
      Lazy<Set<ContainerResponseFilter>> containerResponseFilters,
      Lazy<Set<ExceptionMapper<?>>> exceptionMappers,
      Lazy<Set<Binder>> binders
      // Lazy<Set<DynamicFeature>> dynamicFeatures
      ) {
    // super(context, context.createFilter(ANY_SERVICE_FILTER), null);
    // this.server = dw.getApplicationContext();

    this.authProviders = authProviders;
    this.endpoints = endpoints;
    this.containerRequestFilters = containerRequestFilters;
    this.containerResponseFilters = containerResponseFilters;
    this.exceptionMappers = exceptionMappers;
    this.binders = binders;
    this.providerCache = new ArrayList<>();
    this.filterCache = new ArrayList<>();
    this.changeListeners = new ArrayList<>();
    this.consumers = consumers;
    /*
        if (server.isAvailable()) {
          isJerseyAvailable = true;
          clearConfig();
        }
        server.addLifeCycleListener(this);

        this.dw = dw;
    */
    // TODO
    this.isAuthProviderAvailable = false;
  }

  // should be last
  @Override
  public int getPriority() {
    return 10000;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    jerseyChanged(environment);
  }

  /*
    public synchronized void addingService() {
      Object service = null; // context.getService(reference);

      if (isRegisterable(service)) {
        if (isResource(service)) {
          jerseyRegisterResource(service);
        } else if (isFilter(service)) {
          jerseyAddFilter(service);
        } else if (isProvider(service)) {
          String type = null;
          int ranking = 0;
          try {
            // type = (String) reference.getProperty("provider.type");
            // ranking = (Integer) reference.getProperty("service.ranking");
          } catch (NullPointerException e) {
          }
          if (type != null && type.equals("auth")) {
            registerAuthProvider(service, type, ranking);
          } else {
            jerseyRegisterProvider(service);
          }
        }
        jerseyChanged();
      }
    }

    @Override
    public synchronized void addService(Object service) {

      if (isRegisterable(service)) {
        if (isResource(service)) {
          jerseyRegisterResource(service);
        } else if (isFilter(service)) {
          jerseyAddFilter(service);
        } else if (isProvider(service)) {
          jerseyRegisterProvider(service);
        }
        jerseyChanged();
      }
    }
  */
  @Override
  public synchronized Set<Object> getResources() {
    /*if (isJerseyAvailable) {
      return Sets.union(jersey.getInstances(), jersey.getSingletons());
    }*/
    return new HashSet<>();
  }

  @Override
  public void addChangeListener(JaxRsChangeListener changeListener) {
    changeListeners.add(changeListener);
  }
  /*
    public synchronized void removedService() {
      Object service = null; // context.getService(reference);

      if (isRegisterable(service)) {
        boolean removed = false;
        if (isResource(service)) {
          removed = jerseyUnregister(service);
        } else if (isFilter(service)) {
          removed = jerseyRemoveFilter(service);
        } else if (isProvider(service)) {
          String type = null;
          int ranking = 0;
          try {
            // type = (String) reference.getProperty("provider.type");
            // ranking = (Integer) reference.getProperty("service.ranking");
          } catch (NullPointerException e) {
          }
          if (type != null && type.equals("auth")) {
            removed = deregisterAuthProvider(service, type, ranking);
          } else {
            removed = jerseyUnregister(service);
          }
        }
        if (removed) {
          jerseyChanged();
        }
      }

      // context.ungetService(reference);
    }
  */
  private boolean isJerseyAvailable() {
    return true; // isJerseyAvailable && server.isAvailable();
  }

  private synchronized void jerseyChanged(Environment environment) {
    JerseyEnvironment jersey = environment.jersey();
    if (isJerseyAvailable()) {
      if (!providerCache.isEmpty()) {
        for (Object provider : providerCache) {
          jersey.register(provider);
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(MARKER.DI, "Registered JAX-RS Provider {}", provider.getClass());
        }
        providerCache.clear();
      }
      for (AuthProvider<?> provider : authProviders.get()) {
        jersey.register(provider.getAuthDynamicFeature());
        jersey.register(provider.getRolesAllowedDynamicFeature());
        jersey.register(provider.getAuthValueFactoryProvider());
        this.isAuthProviderAvailable = true;
        if (LOGGER.isDebugEnabled(MARKER.DI))
          LOGGER.debug(MARKER.DI, "Registered JAX-RS Auth Provider {}", provider.getClass());
      }
      if (isAuthProviderAvailable && !endpoints.get().isEmpty()) {
        for (Object resource : endpoints.get()) {
          jersey.register(resource);
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(MARKER.DI, "Registered JAX-RS Resource {}", resource.getClass());
        }
        // endpoints.clear();
      } else if (!isAuthProviderAvailable && !endpoints.get().isEmpty()) {
        if (LOGGER.isDebugEnabled(MARKER.DI))
          LOGGER.debug(
              MARKER.DI, "No JAX-RS Auth Provider registered yet, cannot register Resources.");
      }
      // if (!filterCache.isEmpty()) {
      for (Object filter : filterCache) {
        if (filter instanceof DynamicFeature) {
          // TODO: verify
          jersey.register(filter);
          // jersey.getResourceConfig().getContainerResponseFilters().add(filter.getClass());
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(MARKER.DI, "Registered JAX-RS DynamicFeature {})", filter.getClass());
        }
      }
      // filterCache.clear();
      for (ContainerRequestFilter filter : containerRequestFilters.get()) {
        jersey.register(filter);
        // jersey.getResourceConfig().register()
        // .getContainerRequestFilters().add(filter.getClass());
        if (LOGGER.isDebugEnabled(MARKER.DI))
          LOGGER.debug(
              MARKER.DI, "Registered JAX-RS ContainerRequestFilter {})", filter.getClass());
      }
      for (ContainerResponseFilter filter : containerResponseFilters.get()) {
        jersey.register(filter);
        // jersey.getResourceConfig().getContainerResponseFilters().add(filter.getClass());
        if (LOGGER.isDebugEnabled(MARKER.DI))
          LOGGER.debug(
              MARKER.DI, "Registered JAX-RS ContainerResponseFilter {})", filter.getClass());
      }
      for (Binder binder : binders.get()) {
        jersey.register(binder);
        if (LOGGER.isDebugEnabled(MARKER.DI))
          LOGGER.debug(MARKER.DI, "Registered JAX-RS Binder {}", binder.getClass());
      }
      for (ExceptionMapper<?> exceptionMapper : exceptionMappers.get()) {
        jersey.register(exceptionMapper);
        if (LOGGER.isDebugEnabled(MARKER.DI))
          LOGGER.debug(
              MARKER.DI, "Registered JAX-RS ExceptionMapper {}", exceptionMapper.getClass());
      }
      // }

      // updateDropwizard();

      // clearConfig();

      for (JaxRsChangeListener changeListener : changeListeners) {
        if (changeListener != null) {
          changeListener.jaxRsChanged();
        }
      }
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
  /*
    // TODO: can be moved to dw
    private void updateDropwizard() {
      Optional<ServletHolder> oldServletHolder =
          Arrays.stream(dw.getApplicationContext().getServletHandler().getServlets())
              .filter(sh -> sh.getName().contains("jersey"))
              .findFirst();

      ServletHolder[] servletHolders =
          Arrays.stream(dw.getApplicationContext().getServletHandler().getServlets())
              .filter(sh -> !sh.getName().contains("jersey"))
              .toArray(ServletHolder[]::new);

      ServletMapping[] servletMappings =
          Arrays.stream(dw.getApplicationContext().getServletHandler().getServletMappings())
              .filter(sm -> !sm.getServletName().contains("jersey"))
              .toArray(ServletMapping[]::new);

      ServletHolder shJersey = new NonblockingServletHolder(new ServletContainer(jersey));

      dw.getApplicationContext().getServletHandler().setServlets(servletHolders);
      dw.getApplicationContext().getServletHandler().setServletMappings(servletMappings);
      dw.getApplicationContext().addServlet(shJersey, dw.getJersey().getUrlPattern());

      oldServletHolder.ifPresent(
          servletHolder -> {
            try {
              servletHolder.doStop();
              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Stopped ServletHolder {})", servletHolder.getName());
              }
            } catch (Exception e) {
              // ignore
            }
          });
      // LOGGER.debug("APP {}", dw.getApplicationContext().dump());
    }

    private void clearConfig() {
      ResourceConfig oldConfig = jersey != null ? jersey : dw.getJersey().getResourceConfig();
      this.jersey = new ResourceConfig(oldConfig);
      // LOGGER.debug("OLD CFG {} NEW CFG {}", oldConfig, jersey);
    }

    private void registerAuthProvider(Object service, String type, int ranking) {
      if (type != null && type.equals("auth")) {
        if (authProviders.isEmpty() || ranking > authProviders.lastKey()) {
          if (!authProviders.isEmpty()) {
            Object oldService = authProviders.get(authProviders.lastKey());
            if (oldService != null && jerseyUnregister(oldService)) {
              if (LOGGER.isDebugEnabled(MARKER.DI))
                LOGGER.debug(
                    MARKER.DI, "Deregistered JAX-RS Auth Provider {})", oldService.getClass());
            }
          }
          jerseyRegisterProvider(service);
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(MARKER.DI, "Registered JAX-RS Auth Provider {})", service.getClass());
        }
        authProviders.put(ranking, service);
        isAuthProviderAvailable = true;
      }
    }

    private boolean deregisterAuthProvider(Object service, String type, int ranking) {
      boolean reload = false;
      if (type != null && type.equals("auth")) {
        if (ranking == authProviders.lastKey()) {
          if (jerseyUnregister(service)) {
            if (LOGGER.isDebugEnabled(MARKER.DI))
              LOGGER.debug(MARKER.DI, "Deregistered JAX-RS Auth Provider {})", service.getClass());
          }
          authProviders.remove(ranking);
          if (!authProviders.isEmpty()) {
            Object newService = authProviders.get(authProviders.lastKey());
            if (newService != null) {
              jerseyRegisterProvider(newService);
              if (LOGGER.isDebugEnabled(MARKER.DI))
                LOGGER.debug(MARKER.DI, "Registered JAX-RS Auth Provider {})", newService.getClass());
            }
          }
          reload = true;
        } else {
          authProviders.remove(ranking);
        }
      }

      return reload;
    }
    private void jerseyRegisterResource(Object object) {
      endpoints.add(object);
    }

    private void jerseyRegisterProvider(Object object) {
      providerCache.add(object);
    }

    private boolean jerseyUnregister(Object object) {
      if (isJerseyAvailable) {
        if (jersey.getSingletons().remove(object)) {
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(MARKER.DI, "Unregistered JAX-RS Resource/Provider {})", object.getClass());
          return true;
        }
      } else {
        if (providerCache.contains(object)) {
          providerCache.remove(object);
        }
        if (endpoints.contains(object)) {
          endpoints.remove(object);
        }
      }

      return false;
    }

    private void jerseyAddFilter(Object object) {
      filterCache.add(object);
    }

    private boolean jerseyRemoveFilter(Object object) {
      if (isJerseyAvailable) {
        // TODO: verify
        if (jersey.getClasses().remove(object.getClass())) {
          // if (jersey.getResourceConfig().getContainerRequestFilters().remove(object.getClass())) {
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(
                MARKER.DI, "Unregistered JAX-RS ContainerRequestFilter {})", object.getClass());
          return true;
        } else // TODO: verify
        if (jersey.getClasses().remove(object.getClass())) {
          // if (jersey.getResourceConfig().getContainerResponseFilters().remove(object.getClass())) {
          if (LOGGER.isDebugEnabled(MARKER.DI))
            LOGGER.debug(
                MARKER.DI, "Unregistered JAX-RS ContainerResponseFilter {})", object.getClass());
          return true;
        }
      } else {
        if (filterCache.contains(object)) {
          filterCache.remove(object);
        }
      }

      return false;
    }

  */
  private boolean isRegisterable(Object service) {
    return isResource(service) || isProvider(service) || isFilter(service);
  }

  private boolean isResource(Object service) {
    return service != null && (hasRegisterableAnnotation(service));
  }

  private boolean isProvider(Object service) {
    return service != null
        && (service.getClass().isAnnotationPresent(Provider.class)
            || service instanceof AuthProvider);
  }

  private boolean isFilter(Object service) {
    return service != null
        && (service instanceof ContainerRequestFilter
            || service instanceof ContainerResponseFilter
            || service instanceof DynamicFeature);
  }

  private boolean hasRegisterableAnnotation(Object service) {
    boolean result = isRegisterableAnnotationPresent(service.getClass());
    if (!result) {
      Class<?>[] interfaces = service.getClass().getInterfaces();
      for (Class<?> type : interfaces) {
        result = result || isRegisterableAnnotationPresent(type);
      }
    }
    return result;
  }

  private boolean isRegisterableAnnotationPresent(Class<?> type) {
    return type.isAnnotationPresent(Path.class);
  }
  /*
   @Override
   public void lifeCycleStarting(LifeCycle event) {}

   @Override
   public void lifeCycleStarted(LifeCycle event) {
     isJerseyAvailable = true;
     clearConfig();
     jerseyChanged();
   }

   @Override
   public void lifeCycleFailure(LifeCycle event, Throwable cause) {}

   @Override
   public void lifeCycleStopping(LifeCycle event) {
     isJerseyAvailable = false;
   }

   @Override
   public void lifeCycleStopped(LifeCycle event) {}

  */
}
