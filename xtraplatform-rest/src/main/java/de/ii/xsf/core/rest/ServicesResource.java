/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.rest;

import com.fasterxml.jackson.databind.util.JSONPObject;
import de.ii.xsf.core.api.*;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.core.api.rest.ServiceResourceFactory;
import de.ii.xsf.core.views.GenericView;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.logging.XSFLogger;
import io.dropwizard.views.View;
import io.swagger.oas.annotations.Operation;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.glassfish.jersey.server.ExtendedResourceContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
@Component
@Provides(specifications = {ServicesResource.class})
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xsf.core.api.rest.ServiceResourceFactory)",
        onArrival = "onServiceResourceArrival",
        onDeparture = "onServiceResourceDeparture")

@Path("/services/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class ServicesResource {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(ServicesResource.class);
    @Context
    ExtendedResourceContext rc;
    @Context
    UriInfo uriInfo;
    @Context
    HttpServletRequest request;

    @Requires
    private ServiceRegistry serviceRegistry;

    // TODO
    @Requires(optional = true)
    private AuthorizationProvider permProvider;

    @Requires
    Dropwizard dropwizard;

    private Map<String, ServiceResourceFactory> serviceResourceFactories;

    @org.apache.felix.ipojo.annotations.Context
    private BundleContext context;

    public synchronized void onServiceResourceArrival(ServiceReference<ServiceResourceFactory> ref) {
        ServiceResourceFactory sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResourceFactories.put(type, sr);
        }
    }

    public synchronized void onServiceResourceDeparture(ServiceReference<ServiceResourceFactory> ref) {
        ServiceResourceFactory sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResourceFactories.remove(type);
        }
    }

    public ServicesResource() {
        this.serviceResourceFactories = new HashMap<>();
    }

    @GET
    @Operation
    public Response getServices(@Auth(required = false) AuthenticatedUser authUser, @QueryParam("callback") String callback) {
        if (serviceResourceFactories.size() == 1) {
            Response response = serviceResourceFactories.values().iterator().next().getResponseForParams(serviceRegistry.getServices(authUser), uriInfo);
            if (response != null) {
                return response;
            }
        }

        ServiceCatalog catalog = new ArcGisServiceCatalog(serviceRegistry.getServices(authUser));

        if (callback != null && !callback.isEmpty()) {
            return Response.ok(new JSONPObject(callback, catalog), MediaType.APPLICATION_JSON).build();
        }
        return Response.ok(catalog, MediaType.APPLICATION_JSON).build();
    }

    /*@POST
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Object getServicesPost(@Auth(required = false) AuthenticatedUser user, String body) {
        LOGGER.debug(FrameworkMessages.POST, body);

        return null;//;services;
    }*/

    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getServicesHtml(@Auth(required = false) AuthenticatedUser authUser, @QueryParam("token") String token) {
        if (serviceResourceFactories.size() == 1) {
            Response response = serviceResourceFactories.values().iterator().next().getResponseForParams(serviceRegistry.getServices(authUser), uriInfo);
            if (response != null) {
                return response;
            }

            View view = serviceResourceFactories.values().iterator().next().getServicesView(serviceRegistry.getServices(authUser), uriInfo.getRequestUri());
            if (view != null) {
                return Response.ok(view).build();
            }
        }
        return Response.ok(new GenericView("services", uriInfo.getRequestUri(), serviceRegistry.getServices(authUser))).build();
    }

    @Path("/{service}/")
    public ServiceResource getServiceResource(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser authUser,*/ @PathParam("service") String id, @QueryParam("callback") String callback) {
        try {
            MDC.put("service", id);
            return getServiceResource(getService(new AuthenticatedUser(), id, callback));
        } finally {
            MDC.remove("service");
        }
    }

    // TODO: after switch to jersey 2.x, use Resource.from and move instantiation to factory 
    // TODO: cache resource object per service
    private ServiceResource getServiceResource(Service s) {
        ServiceResourceFactory factory = serviceResourceFactories.get(s.getType());
        //ServiceResource sr = (ServiceResource) rc.getResource(factory.getServiceResourceClass());
        ServiceResource sr = rc.initResource(factory.getServiceResource());
        sr.setService(s);
        sr.init(permProvider);
        sr.setMustacheRenderer(dropwizard.getMustacheRenderer());
        return sr;
    }

    private Service getService(AuthenticatedUser authUser, String id, String callback) {
        Service s = serviceRegistry.getService(authUser, id);

        if (s == null || !s.isStarted()) {
            throw new ResourceNotFound(/*FrameworkMessages.A_SERVICE_WITH_ID_ID_IS_NOT_AVAILABLE.get(id).toString(LOGGER.getLocale()),*/ callback);
        }

        return s;
    }

    // TODO: move to XtraProxy
    /*@Path("/{service}/FeatureServer")
    public ServiceResource getServiceFS(@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user, @PathParam("service") String service, @QueryParam("callback") String callback) {
        return getServiceResource(user, service, callback);
    }*/

}
