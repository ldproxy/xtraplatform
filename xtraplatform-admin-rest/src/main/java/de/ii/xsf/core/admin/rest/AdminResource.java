/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.admin.rest;

import de.ii.xsf.cfgstore.api.LocalBundleConfigStore;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.Module;
import de.ii.xsf.core.api.ModulesRegistry;
import de.ii.xsf.core.api.ServiceRegistry;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.rest.AdminModuleResource;
import de.ii.xsf.core.api.rest.AdminModuleResourceFactory;
import de.ii.xsf.core.api.rest.ModuleResource;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xtraplatform.entity.api.EntityRegistry;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.entity.api.EntityRepositoryForType;
import de.ii.xtraplatform.service.api.AdminServiceResource;
import de.ii.xtraplatform.service.api.ImmutableServiceDataWithStatus;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceDataWithStatus;
import de.ii.xtraplatform.service.api.ServiceResource;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.glassfish.jersey.server.ExtendedResourceContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author zahnen
 */
@Component
@Provides(specifications = {AdminResource.class})
@Instantiate
@Whiteboards(whiteboards = {
        @Wbp(
                filter = "(objectClass=de.ii.xtraplatform.service.api.AdminServiceResource)",
                onArrival = "onServiceResourceArrival",
                onDeparture = "onServiceResourceDeparture"),
    @Wbp(
            filter = "(objectClass=de.ii.xsf.core.api.rest.AdminModuleResourceFactory)",
            onArrival = "onModuleResourceArrival",
            onDeparture = "onModuleResourceDeparture")
})

@Path("/admin/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class AdminResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminResource.class);

    @Requires
    private ModulesRegistry modulesRegistry;
    @Requires
    private ServiceRegistry serviceRegistry;
    @Requires
    private Jackson jackson;
    // TODO
    @Requires(optional = true)
    private AuthorizationProvider permissions;

    @Requires
    private EntityRegistry entityRegistry;
    @Requires
    private EntityRepository entityRepository;

    @Requires
    private ServiceDataInjectableContext serviceDataContext;



    private String xsfVersion = "todo";

    @Context
    ExtendedResourceContext rc;

    Map<String, AdminServiceResource> serviceResources;
    Map<String, AdminModuleResourceFactory> moduleResourceFactories;

    @org.apache.felix.ipojo.annotations.Context
    private BundleContext context;

    public synchronized void onServiceResourceArrival(ServiceReference<AdminServiceResource> ref) {
        AdminServiceResource sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResources.put(type, sr);
        }
    }

    public synchronized void onServiceResourceDeparture(ServiceReference<AdminServiceResource> ref) {
        AdminServiceResource sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            serviceResources.remove(type);
        }
    }

    public synchronized void onModuleResourceArrival(ServiceReference<AdminModuleResourceFactory> ref) {
        AdminModuleResourceFactory sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            moduleResourceFactories.put(type, sr);
        }
    }

    public synchronized void onModuleResourceDeparture(ServiceReference<AdminModuleResourceFactory> ref) {
        AdminModuleResourceFactory sr = context.getService(ref);
        String type = (String) ref.getProperty(ServiceResource.SERVICE_TYPE_KEY);
        if (sr != null && type != null) {
            moduleResourceFactories.remove(type);
        }
    }

    public AdminResource() {
        this.serviceResources = new HashMap<>();
        this.moduleResourceFactories = new HashMap<>();
    }

    @GET
    @CacheControl(noCache = true)
    public AdminRoot getAdmin() {
        return new AdminRoot(xsfVersion);
    }

    @Path("/services")
    @GET
    @CacheControl(noCache = true)
    public List getAdminServices(@Auth AuthenticatedUser authUser) {
        return new EntityRepositoryForType(entityRepository, Service.ENTITY_TYPE).getEntityIds();
        /*return entityRegistry.getEntitiesForType(Service.class, Service.ENTITY_TYPE)
                      .stream()
                      .map(Service::getId)
                      .collect(Collectors.toList());*/
    }

    @Path("/modules")
    @GET
    @CacheControl(noCache = true)
    public Set<String> getModules() {
        return modulesRegistry.getModules().keySet();
    }

    @Path("/modules/{id}")
    @CacheControl(noCache = true)
    public ModuleResource getModule(@PathParam("id") String id) {
        Module m = modulesRegistry.getModule(id);

        if (m == null) {
            throw new ResourceNotFound(/*FrameworkMessages.A_MODULE_WITH_ID_ID_IS_NOT_AVAILABLE.get(id).toString(LOGGER.getLocale())*/);
        }

        return getAdminModuleResource(m);
    }

    // TODO: after switch to jersey 2.x, use Resource.from and move instantiation to factory 
    // TODO: cache resource object per service
    private AdminModuleResource getAdminModuleResource(Module m) {
        AdminModuleResourceFactory factory = moduleResourceFactories.get(m.getName());
        if (factory == null) {
            throw new ResourceNotFound();
        }
        AdminModuleResource mr = (AdminModuleResource) rc.getResource(factory.getAdminModuleResourceClass());
        mr.init(m, permissions);
        return mr;
    }

    @Path("/servicetypes")
    @GET
    @CacheControl(noCache = true)
    public Collection getAdminServiceTypes() {
        return serviceRegistry.getServiceTypes();
    }

    @Path("/services")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addService(/*@Auth(minRole = Role.PUBLISHER) AuthenticatedUser authUser,*/ Map<String, Object> request) {
        try {
            MDC.put("service", (String) request.get("id"));
            new EntityRepositoryForType(entityRepository, Service.ENTITY_TYPE).generateEntity(request);
            //serviceRegistry.addService(/*authUser*/new AuthenticatedUser(), request.get("type"), request.get("id"), request);
            return Response.ok().build();
        } catch (IOException e) {
            LOGGER.error("Error adding service", e);
            throw new BadRequestException();
        } finally {
            MDC.remove("service");
        }
    }

    @Path("/services/{id}")
    public AdminServiceResource getAdminService(/*@Auth AuthenticatedUser authUser,*/ @PathParam("id") String id, @Context ContainerRequestContext containerRequestContext) {

        //Service s = serviceRegistry.getService(/*authUser*/new AuthenticatedUser(), id);
        //Optional<Service> service = entityRegistry.getEntity(Service.class, Service.ENTITY_TYPE, id);
        ServiceData serviceData = (ServiceData) new EntityRepositoryForType(entityRepository, Service.ENTITY_TYPE).getEntityData(id);

        //Service service = getService(new AuthenticatedUser(), id);
        serviceDataContext.inject(containerRequestContext, serviceData);

        if (Objects.isNull(serviceData)) {
            throw new ResourceNotFound(/*FrameworkMessages.A_SERVICE_WITH_ID_ID_IS_NOT_AVAILABLE.get(id).toString(LOGGER.getLocale())*/);
        }

        AdminServiceResource sr = getAdminServiceResource(serviceData);

        return sr;
    }

    @Requires
    LocalBundleConfigStore localBundleConfigStore;

    @Path("/settings")
    @GET
    @CacheControl(noCache = true)
    public Map<String, Object> getSettingCategories(/*@Auth AuthenticatedUser authUser*/) {
        return localBundleConfigStore.getCategories();
    }

    @Path("/settings/{category}")
    @GET
    @CacheControl(noCache = true)
    public Map<String, Object> getSettingCategory(/*@Auth AuthenticatedUser authUser,*/ @PathParam("category") String category) {
        if (!localBundleConfigStore.hasCategory(category)) {
            throw new ResourceNotFound();
        }

        return localBundleConfigStore.getConfigProperties(category);
    }

    @Path("/settings/{category}")
    @POST
    @CacheControl(noCache = true)
    public Map<String, Object> postSettingCategory(/*@Auth AuthenticatedUser authUser,*/ @PathParam("category") String category, Map<String, String> body) {
        if (!localBundleConfigStore.hasCategory(category)) {
            throw new ResourceNotFound();
        }

        try {
            localBundleConfigStore.updateConfigProperties(category, body);
        } catch (IOException e) {
            throw new NotAcceptableException();
        }

        return localBundleConfigStore.getConfigProperties(category);
    }

    // TODO: after switch to jersey 2.x, use Resource.from and move instantiation to factory
    // TODO: cache resource object per service
    private AdminServiceResource getAdminServiceResource(ServiceData s) {

        AdminServiceResource sr = serviceResources.get(s.getServiceType());
        if (sr == null) {
            throw new ResourceNotFound();
        }

        /*boolean started = entityRegistry.getEntity(Service.class, Service.ENTITY_TYPE, s.getId())
                                        .isPresent();

        ServiceDataWithStatus serviceDataWithStatus = ImmutableServiceDataWithStatus.builder().from(s).status(started ? ServiceDataWithStatus.STATUS.STARTED : ServiceDataWithStatus.STATUS.STOPPED).build();*/

        //AbstractAdminServiceResource sr = factory.getAdminServiceResource();//(AdminServiceResource) rc.getResource(factory.getAdminServiceResourceClass());
        //sr.setService(s);
        //sr.init(jackson.getDefaultObjectMapper(), new EntityRepositoryForType(entityRepository, Service.ENTITY_TYPE), permissions, serviceDataWithStatus);

        return sr;
    }

    private Service getService(AuthenticatedUser authUser, String id) {
        //Service s = serviceRegistry.getService(authUser, id);
        Optional<Service> s = entityRegistry.getEntity(Service.class, Service.ENTITY_TYPE, id);

        if (!s.isPresent() /*|| !s.isStarted()*/) {
            throw new ResourceNotFound(/*FrameworkMessages.A_SERVICE_WITH_ID_ID_IS_NOT_AVAILABLE.get(id).toString(LOGGER.getLocale()),*/);
        }

        return s.get();
    }

    private Map<String, String> flattenMultiMap(MultivaluedMap<String, String> multiMap) {
        Map<String, String> flatMap = new HashMap<>();
        for (String key : multiMap.keySet()) {
            flatMap.put(key, multiMap.getFirst(key));
        }
        return flatMap;
    }

}
