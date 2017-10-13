/**
 * Copyright 2017 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.web;

import com.google.common.collect.Sets;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.logging.XSFLogger;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.NonblockingServletHolder;
import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.component.LifeCycle;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author zahnen
 */
// used basic idea from https://github.com/hstaudacher/osgi-jax-rs-connector
//
@Component
@Provides
@Instantiate
@Wbp(
        filter = JaxRsRegistry.ANY_SERVICE_FILTER,
        onArrival = "addingService",
        onDeparture = "removedService")

public class JaxRsRegistry implements LifeCycle.Listener, JaxRsReg {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(JaxRsRegistry.class);
    public static final String PUBLISH = "de.ii.xsf.jaxrs.publish";
    public static final String ANY_SERVICE_FILTER = "(&(objectClass=*)(!(" + PUBLISH + "=false)))";

    private final BundleContext context;
    private final MutableServletContextHandler server;
    private final SortedMap<Integer, Object> authProviders;
    private final List<Object> resourceCache;
    private final List<Object> providerCache;
    private final List<Object> filterCache;
    private boolean isJerseyAvailable;
    private boolean isAuthProviderAvailable;
    private final Dropwizard dw;
    private ResourceConfig jersey;
    private final List<JaxRsChangeListener> changeListeners;

    JaxRsRegistry(@Context BundleContext context, @Requires Dropwizard dw) {
        //super(context, context.createFilter(ANY_SERVICE_FILTER), null);
        this.context = context;
        this.server = dw.getApplicationContext();

        this.authProviders = new TreeMap<>();
        this.resourceCache = new ArrayList<>();
        this.providerCache = new ArrayList<>();
        this.filterCache = new ArrayList<>();
        this.changeListeners = new ArrayList<>();

        if (server.isAvailable()) {
            isJerseyAvailable = true;
        }
        server.addLifeCycleListener(this);

        this.dw = dw;

        // TODO
        this.isAuthProviderAvailable = true;

        clearConfig();
    }

    public synchronized void addingService(ServiceReference reference) {
        Object service = context.getService(reference);

        if (isRegisterable(service)) {
            if (isResource(service)) {
                jerseyRegisterResource(service);
            } else if (isFilter(service)) {
                jerseyAddFilter(service);
            } else if (isProvider(service)) {
                String type = null;
                int ranking = 0;
                try {
                    type = (String) reference.getProperty("provider.type");
                    ranking = (Integer) reference.getProperty("service.ranking");
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

    @Override
    public synchronized Set<Object> getResources() {
        if (isJerseyAvailable) {
            return Sets.union(jersey.getInstances(), jersey.getSingletons());
        }
        return new HashSet<>();
    }

    @Override
    public void addChangeListener(JaxRsChangeListener changeListener) {
        changeListeners.add(changeListener);
    }

    public synchronized void removedService(ServiceReference reference) {
        Object service = context.getService(reference);

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
                    type = (String) reference.getProperty("provider.type");
                    ranking = (Integer) reference.getProperty("service.ranking");
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

        context.ungetService(reference);
    }

    private boolean isJerseyAvailable() {
        return isJerseyAvailable && server.isAvailable();
    }

    private synchronized void jerseyChanged() {
        if (isJerseyAvailable()) {
            if (!providerCache.isEmpty()) {
                for (Object provider : providerCache) {
                    jersey.register(provider);
                    LOGGER.getLogger().debug("Registered JAX-RS Provider {}", provider.getClass());
                }
                providerCache.clear();
            }
            if (isAuthProviderAvailable && !resourceCache.isEmpty()) {
                for (Object resource : resourceCache) {
                    jersey.register(resource);
                    LOGGER.getLogger().debug("Registered JAX-RS Resource {}", resource.getClass());
                }
                resourceCache.clear();
            }
            if (!filterCache.isEmpty()) {
                for (Object filter : filterCache) {
                    if (filter instanceof ContainerRequestFilter) {
                        // TODO: verify
                        jersey.register(filter.getClass());
                        //jersey.getResourceConfig().register() .getContainerRequestFilters().add(filter.getClass());
                        LOGGER.getLogger().debug("Registered JAX-RS ContainerRequestFilter {})", filter.getClass());
                    } else if (filter instanceof ContainerResponseFilter) {
                        // TODO: verify
                        jersey.register(filter.getClass());
                        //jersey.getResourceConfig().getContainerResponseFilters().add(filter.getClass());
                        LOGGER.getLogger().debug("Registered JAX-RS ContainerResponseFilter {})", filter.getClass());
                    }
                }
                filterCache.clear();
            }

            updateDropwizard();

            clearConfig();

            for (JaxRsChangeListener changeListener : changeListeners) {
                if (changeListener != null) {
                    changeListener.jaxRsChanged();
                }
            }
        }
    }

    // TODO: can be moved to dw
    private void updateDropwizard() {
        Optional<ServletHolder> oldServletHolder = Arrays.stream(dw.getApplicationContext().getServletHandler().getServlets())
                .filter(sh -> sh.getName().contains("jersey"))
                .findFirst();

        ServletHolder[] servletHolders = Arrays.stream(dw.getApplicationContext().getServletHandler().getServlets())
                .filter(sh -> !sh.getName().contains("jersey"))
                .toArray(ServletHolder[]::new);

        ServletMapping[] servletMappings = Arrays.stream(dw.getApplicationContext().getServletHandler().getServletMappings())
                .filter(sm -> !sm.getServletName().contains("jersey"))
                .toArray(ServletMapping[]::new);

        ServletHolder shJersey = new NonblockingServletHolder(new ServletContainer(jersey));

        dw.getApplicationContext().getServletHandler().setServlets(servletHolders);
        dw.getApplicationContext().getServletHandler().setServletMappings(servletMappings);
        dw.getApplicationContext().addServlet(shJersey, dw.getJersey().getUrlPattern());

        oldServletHolder
                .ifPresent(servletHolder -> {
                    try {
                        servletHolder.doStop();
                        LOGGER.getLogger().debug("Stopped ServletHolder {})", servletHolder.getName());
                    } catch (Exception e) {
                        // ignore
                    }
                });
        //LOGGER.getLogger().debug("APP {}", dw.getApplicationContext().dump());
    }

    private void clearConfig() {
        ResourceConfig oldConfig = jersey != null ? jersey : dw.getJersey().getResourceConfig();
        this.jersey = new ResourceConfig(oldConfig);
        //LOGGER.getLogger().debug("OLD CFG {} NEW CFG {}", oldConfig, jersey);
    }

    private void registerAuthProvider(Object service, String type, int ranking) {
        if (type != null && type.equals("auth")) {
            if (authProviders.isEmpty() || ranking > authProviders.lastKey()) {
                if (!authProviders.isEmpty()) {
                    Object oldService = authProviders.get(authProviders.lastKey());
                    if (oldService != null && jerseyUnregister(oldService)) {
                        LOGGER.getLogger().debug("Deregistered JAX-RS Auth Provider {})", oldService.getClass());
                    }
                }
                jerseyRegisterProvider(service);
                LOGGER.getLogger().debug("Registered JAX-RS Auth Provider {})", service.getClass());
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
                    LOGGER.getLogger().debug("Deregistered JAX-RS Auth Provider {})", service.getClass());
                }
                authProviders.remove(ranking);
                if (!authProviders.isEmpty()) {
                    Object newService = authProviders.get(authProviders.lastKey());
                    if (newService != null) {
                        jerseyRegisterProvider(newService);
                        LOGGER.getLogger().debug("Registered JAX-RS Auth Provider {})", newService.getClass());
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
        resourceCache.add(object);
    }

    private void jerseyRegisterProvider(Object object) {
        providerCache.add(object);
    }

    private boolean jerseyUnregister(Object object) {
        if (isJerseyAvailable) {
            if (jersey.getSingletons().remove(object)) {
                LOGGER.getLogger().debug("Unregistered JAX-RS Resource/Provider {})", object.getClass());
                return true;
            }
        } else {
            if (providerCache.contains(object)) {
                providerCache.remove(object);
            }
            if (resourceCache.contains(object)) {
                resourceCache.remove(object);
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
                //if (jersey.getResourceConfig().getContainerRequestFilters().remove(object.getClass())) {
                LOGGER.getLogger().debug("Unregistered JAX-RS ContainerRequestFilter {})", object.getClass());
                return true;
            } else // TODO: verify
                if (jersey.getClasses().remove(object.getClass())) {
                    //if (jersey.getResourceConfig().getContainerResponseFilters().remove(object.getClass())) {
                    LOGGER.getLogger().debug("Unregistered JAX-RS ContainerResponseFilter {})", object.getClass());
                    return true;
                }
        } else {
            if (filterCache.contains(object)) {
                filterCache.remove(object);
            }
        }

        return false;
    }

    private boolean isRegisterable(Object service) {
        return isResource(service) || isProvider(service) || isFilter(service);
    }

    private boolean isResource(Object service) {
        return service != null && (hasRegisterableAnnotation(service));
    }

    private boolean isProvider(Object service) {
        return service != null && service.getClass().isAnnotationPresent(Provider.class);
    }

    private boolean isFilter(Object service) {
        return service != null && (service instanceof ContainerRequestFilter || service instanceof ContainerResponseFilter);
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

    @Override
    public void lifeCycleStarting(LifeCycle event) {

    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        isJerseyAvailable = true;
        jerseyChanged();
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {

    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        isJerseyAvailable = false;
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {

    }
}
