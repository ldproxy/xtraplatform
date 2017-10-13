/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.web;

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
    //private final JerseyEnvironment jersey;
    //private final ServletContainer jerseyContainer;
    private final SortedMap<Integer, Object> authProviders;
    private final List<Object> resourceCache;
    private final List<Object> providerCache;
    private final List<Object> filterCache;
    private boolean isJerseyAvailable;
    private boolean isAuthProviderAvailable;
    private final Dropwizard dw;
    private ResourceConfig jersey;
    private ResourceConfig resourceConfig;

    JaxRsRegistry(@Context BundleContext context, @Requires Dropwizard dw) {
        //super(context, context.createFilter(ANY_SERVICE_FILTER), null);
        this.context = context;
        this.server = dw.getApplicationContext();
        //this.jersey = dw.getJersey();
        //this.jerseyContainer = dw.getJerseyContainer();

        this.authProviders = new TreeMap<>();
        this.resourceCache = new ArrayList<>();
        this.providerCache = new ArrayList<>();
        this.filterCache = new ArrayList<>();

        // workaround: reload calls init(scanner) and scanner is null without this
        String[] pkgs = {"does.not.exist"};
        //jersey.packages(pkgs);

        if (server.isAvailable()) {
            isJerseyAvailable = true;
        }
        server.addLifeCycleListener(this);
        
        this.dw = dw;

        // TODO
        this.isAuthProviderAvailable = true;
        this.resourceConfig = dw.getJersey().getResourceConfig();
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

            LOGGER.getLogger().debug("OLD RC {} {}", dw.getJerseyContainer(), dw.getJerseyContainer().getConfiguration().getClasses());

            final ServletContainer sc = new ServletContainer(jersey);

            /*dw.getEnvironment().jersey().replace(new Function<ResourceConfig, Servlet>() {
                @Nullable
                @Override
                public Servlet apply(@Nullable ResourceConfig input) {
                    try {
                        //sc.init(dw.getJerseyContainer().getServletConfig());
                        LOGGER.getLogger().debug("NEW RC {} {}", sc, jersey.getClasses());
                    } catch (ServletException e) {
                        //ignore
                    }

                    return sc;
                }
            });*/

            //jersey.register(new JacksonBinder(dw.getEnvironment().getObjectMapper()));

            /*jersey.register(OpenApiResource.class);

            List<Class<?>> cl = new ArrayList<>();
            for (Class c : jersey.getClasses()) {
                //cl.add(c.getPackage().getName());
            }
            for (Object c : jersey.getInstances()) {
                //cl.add(c.getClass().getPackage().getName());
                cl.add(c.getClass());
            }
            LOGGER.getLogger().debug("Registered JAX-RS CL {}", cl);

            OpenAPI oas = new OpenAPI();
            Info info = new Info()
                    .title("Swagger Sample App")
                    .description("This is a sample server Petstore server.  You can find out more about Swagger " +
                            "at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, " +
                            "you can use the api key `special-key` to test the authorization filters.")
                    .termsOfService("http://swagger.io/terms/")
                    .contact(new Contact()
                            .email("apiteam@swagger.io"))
                    .license(new License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

            oas.info(info);




            SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                    .openAPI(oas);
            //        .resourcePackages(Sets.newHashSet(cl));

            Reader reader = new Reader(oasConfig);
            reader.read(jersey.getClasses());
            reader.read(Sets.newHashSet(cl));

            try {
                new JaxrsOpenApiContextBuilder()
                        .servletConfig(sc.getServletConfig())
                        .openApiConfiguration(oasConfig)
                        .buildContext(true);
            } catch (OpenApiConfigurationException e) {
                LOGGER.getLogger().debug("ERR", e);
                //throw new ServletException(e.getMessage(), e);
            }*/






ServletHolder[] servletHolders = Arrays.<ServletHolder>copyOf(dw.getApplicationContext().getServletHandler().getServlets(), dw.getApplicationContext().getServletHandler().getServlets().length -1);
            ServletMapping[] servletMappings = Arrays.<ServletMapping>copyOf(dw.getApplicationContext().getServletHandler().getServletMappings(), dw.getApplicationContext().getServletHandler().getServletMappings().length -1);
ServletHolder shJersey = null;
int j = 0;
            int k = 0;

            for (int i = 0; i < dw.getApplicationContext().getServletHandler().getServlets().length; i++ ) {
                ServletHolder sh = dw.getApplicationContext().getServletHandler().getServlets()[i];

                LOGGER.getLogger().debug("SERVLET {} {}", sh, sh.getName());

                if (sh.getName().contains("jersey")) {
                    try {
                        shJersey = new NonblockingServletHolder(sc);
                        //sh.setServlet(sc);
                        //shJersey.setServletHandler(dw.getApplicationContext().getServletHandler());
                        //shJersey.start();
                        //shJersey.initialize();
                        //LOGGER.getLogger().debug("LINK RC {} {} {} {}", sh, servletHolders[i], sh.getServlet(), ((ServletContainer)sh.getServlet()).getConfiguration().getSingletons());
                    } catch (Exception e) {
                        LOGGER.getLogger().debug("ERR", e);
                    }
                }
                else {
                    servletHolders[j] = sh;
                    j++;
                }

                if (!dw.getApplicationContext().getServletHandler().getServletMappings()[i].containsPathSpec(dw.getJersey().getUrlPattern())) {
                    servletMappings[k] = dw.getApplicationContext().getServletHandler().getServletMappings()[i];
                    k++;
                }
            }
            LOGGER.getLogger().debug("SERVLETS {} {}", servletHolders, servletMappings);

            LOGGER.getLogger().debug("RC {} {} {}", jersey, jersey.getClasses(), jersey.getSingletons());

            dw.getApplicationContext().getServletHandler().setServlets(servletHolders);
            dw.getApplicationContext().getServletHandler().setServletMappings(servletMappings);
            dw.getApplicationContext().addServlet(shJersey, dw.getJersey().getUrlPattern());

            LOGGER.getLogger().debug("APP {}", dw.getApplicationContext().dump());

            clearConfig();
            //dw.getJerseyContainer().reload();

        }
    }

    private void clearConfig() {
        this.jersey = new ResourceConfig(resourceConfig);
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
