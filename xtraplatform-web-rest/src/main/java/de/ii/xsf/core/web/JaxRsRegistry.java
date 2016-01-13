/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xsf.core.web;

import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.logging.XSFLogger;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.eclipse.jetty.util.component.LifeCycle;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
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

public class JaxRsRegistry implements LifeCycle.Listener {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(JaxRsRegistry.class);
    public static final String PUBLISH = "de.ii.xsf.jaxrs.publish";
    public static final String ANY_SERVICE_FILTER = "(&(objectClass=*)(!(" + PUBLISH + "=false)))";

    private final BundleContext context;
    private final MutableServletContextHandler server;
    private final JerseyEnvironment jersey;
    //private final ServletContainer jerseyContainer;
    private final SortedMap<Integer, Object> authProviders;
    private final List<Object> resourceCache;
    private final List<Object> providerCache;
    private final List<Object> filterCache;
    private boolean isJerseyAvailable;
    private boolean isAuthProviderAvailable;
    private final Dropwizard dw;

    JaxRsRegistry(@Context BundleContext context, @Requires Dropwizard dw) {
        //super(context, context.createFilter(ANY_SERVICE_FILTER), null);
        this.context = context;
        this.server = dw.getApplicationContext();
        this.jersey = dw.getJersey();
        //this.jerseyContainer = dw.getJerseyContainer();

        this.authProviders = new TreeMap<>();
        this.resourceCache = new ArrayList<>();
        this.providerCache = new ArrayList<>();
        this.filterCache = new ArrayList<>();

        // workaround: reload calls init(scanner) and scanner is null without this
        String[] pkgs = {"does.not.exist"};
        jersey.packages(pkgs);

        if (server.isAvailable()) {
            isJerseyAvailable = true;
        }
        server.addLifeCycleListener(this);
        
        this.dw = dw;
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
                    jersey.getResourceConfig().getContainerRequestFilters().add(filter.getClass());
                    LOGGER.getLogger().debug("Registered JAX-RS ContainerRequestFilter {})", filter.getClass());
                }
                filterCache.clear();
            }

            dw.getJerseyContainer().reload();
        }
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
            if (jersey.getResourceConfig().getSingletons().remove(object)) {
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
            if (jersey.getResourceConfig().getContainerRequestFilters().remove(object.getClass())) {
                LOGGER.getLogger().debug("Unregistered JAX-RS ContainerRequestFilter {})", object.getClass());
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
        return service != null && service instanceof ContainerRequestFilter;
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
