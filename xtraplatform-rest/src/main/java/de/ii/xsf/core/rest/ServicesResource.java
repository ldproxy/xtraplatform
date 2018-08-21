/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.rest;

import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.views.GenericView;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.entity.api.EntityRegistry;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceListingProvider;
import de.ii.xtraplatform.service.api.ServiceResource;
import de.ii.xtraplatform.service.api.ServiceResourceFactory;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {ServicesResource.class})
@Instantiate
@Whiteboards(whiteboards = {
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.service.api.ServiceResource)",
                onArrival = "onServiceResourceArrival",
                onDeparture = "onServiceResourceDeparture"),
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.service.api.ServiceResourceFactory)",
                onArrival = "onServiceResourceFactoryArrival",
                onDeparture = "onServiceResourceFactoryDeparture")
})
@Path("/services/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class ServicesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesResource.class);

    //@Requires
    //private ServiceRegistry serviceRegistry;

    @Requires
    private EntityRegistry entityRegistry;

    @Requires
    private ServiceInjectableContext serviceContext;

    // TODO
    @Requires(optional = true)
    private AuthorizationProvider permProvider;

    @Requires(optional = true)
    private ServiceListingProvider serviceListingProvider;

    @Requires
    Dropwizard dropwizard;

    private Map<String, ServiceResourceFactory> serviceResourceFactories;
    private Map<String, ServiceResource> serviceResources;

    @org.apache.felix.ipojo.annotations.Context
    private BundleContext context;

    public synchronized void onServiceResourceArrival(ServiceReference<ServiceResource> ref) {
        ServiceResource sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResources.put(type, sr);
        }
    }

    public synchronized void onServiceResourceDeparture(ServiceReference<ServiceResource> ref) {
        ServiceResource sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResources.remove(type);
        }
    }

    public synchronized void onServiceResourceFactoryArrival(ServiceReference<ServiceResourceFactory> ref) {
        ServiceResourceFactory sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResourceFactories.put(type, sr);
        }
    }

    public synchronized void onServiceResourceFactoryDeparture(ServiceReference<ServiceResourceFactory> ref) {
        ServiceResourceFactory sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResourceFactories.remove(type);
        }
    }

    public ServicesResource() {
        this.serviceResourceFactories = new HashMap<>();
        this.serviceResources = new LinkedHashMap<>();
    }

    //TODO
    @GET
    public Response getServices(@Auth(required = false) AuthenticatedUser authUser, @QueryParam("callback") String callback, @Context UriInfo uriInfo) {
        if (serviceResourceFactories.size() == 1) {
            //Response response = serviceResourceFactories.values().iterator().next().getResponseForParams(serviceRegistry.getServices(authUser), uriInfo);
            Response response = serviceResourceFactories.values()
                                                        .iterator()
                                                        .next()
                                                        .getResponseForParams(entityRegistry.getEntitiesForType(Service.class, Service.ENTITY_TYPE), uriInfo);
            if (response != null) {
                return response;
            }
        }

        /*ServiceCatalog catalog = new ArcGisServiceCatalog(serviceRegistry.getServices(authUser));

        if (callback != null && !callback.isEmpty()) {
            return Response.ok(new JSONPObject(callback, catalog), MediaType.APPLICATION_JSON).build();
        }
        return Response.ok(catalog, MediaType.APPLICATION_JSON).build();*/
        return Response.serverError()
                       .build();
    }

    /*@POST
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Object getServicesPost(@Auth(required = false) AuthenticatedUser user, String body) {
        LOGGER.debug(FrameworkMessages.POST, body);

        return null;//;services;
    }*/

    //TODO
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getServicesHtml(@Auth(required = false) AuthenticatedUser authUser, @QueryParam("token") String token, @Context UriInfo uriInfo) {
        /*if (serviceResourceFactories.size() == 1) {
            Response response = serviceResourceFactories.values()
                                                        .iterator()
                                                        .next()
                                                        .getResponseForParams(entityRegistry.getEntitiesForType(Service.class, Service.ENTITY_TYPE), uriInfo);
            //Response response = serviceResourceFactories.values().iterator().next().getResponseForParams(serviceRegistry.getServices(authUser), uriInfo);
            if (response != null) {
                return response;
            }

            View view = serviceResourceFactories.values()
                                                .iterator()
                                                .next()
                                                .getServicesView(entityRegistry.getEntitiesForType(Service.class, Service.ENTITY_TYPE), uriInfo.getRequestUri());
            //View view = serviceResourceFactories.values().iterator().next().getServicesView(serviceRegistry.getServices(authUser), uriInfo.getRequestUri());
            if (view != null) {
                return Response.ok(view)
                               .build();
            }
        }*/

        List<ServiceData> services = entityRegistry.getEntitiesForType(Service.class, Service.ENTITY_TYPE)
                                                   .stream()
                                                   .map(Service::getData)
                                                   .collect(Collectors.toList());

        if (Objects.nonNull(serviceListingProvider)) {
            return serviceListingProvider.getServiceListing(services, uriInfo.getRequestUri());
        }

        return Response.ok(new GenericView("services", uriInfo.getRequestUri(), entityRegistry.getEntitiesForType(Service.class, Service.ENTITY_TYPE)))
                       .build();
        //return Response.ok(new GenericView("services", uriInfo.getRequestUri(), serviceRegistry.getServices(authUser))).build();
    }

    //TODO
    @GET
    @Path("/___static___/{file: .+}")
    @Produces(MediaType.WILDCARD)
    @CacheControl(maxAge = 3600)
    public Response getFile(@PathParam("file") String file) {
        //LOGGER.debug("FILE {})", file);

        if (Objects.nonNull(serviceListingProvider)) {
            return serviceListingProvider.getStaticAsset(file);
        }

        return Response.status(Response.Status.NOT_FOUND)
                       .build();
    }

    @Path("/{service}/")
    public ServiceResource getServiceResource(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser authUser,*/ @PathParam("service") String id, @QueryParam("callback") String callback, @Context ContainerRequestContext containerRequestContext) {
        try {
            MDC.put("service", id);

            Service service = getService(new AuthenticatedUser(), id, callback);
            serviceContext.inject(containerRequestContext, service);

            return getServiceResource(service);
        } finally {
            MDC.remove("service");
        }
    }

    // TODO: after switch to jersey 2.x, use Resource.from and move instantiation to factory 
    // TODO: cache resource object per service
    private ServiceResource getServiceResource(Service s) {
        /*ServiceResourceFactory factory = serviceResourceFactories.get(s.getServiceType());
        //ServiceResource sr = (ServiceResource) rc.getResource(factory.getServiceResourceClass());
        ServiceResource sr = rc.initResource(factory.getServiceResource());
        sr.setService(s);
        sr.init(permProvider);
        sr.setMustacheRenderer(dropwizard.getMustacheRenderer());
        return sr;*/
        return serviceResources.get(s.getServiceType());
    }

    private Service getService(AuthenticatedUser authUser, String id, String callback) {
        //Service s = serviceRegistry.getService(authUser, id);
        Optional<Service> s = entityRegistry.getEntity(Service.class, Service.ENTITY_TYPE, id);

        if (!s.isPresent() /*|| !s.isStarted()*/) {
            throw new ResourceNotFound(/*FrameworkMessages.A_SERVICE_WITH_ID_ID_IS_NOT_AVAILABLE.get(id).toString(LOGGER.getLocale()),*/ callback);
        }

        return s.get();
    }

    // TODO: move to XtraProxy
    /*@Path("/{service}/FeatureServer")
    public ServiceResource getServiceFS(@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user, @PathParam("service") String service, @QueryParam("callback") String callback) {
        return getServiceResource(user, service, callback);
    }*/

}
