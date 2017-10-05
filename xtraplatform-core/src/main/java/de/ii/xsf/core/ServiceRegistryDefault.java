/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core;

import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.ServiceModule;
import de.ii.xsf.core.api.ServiceRegistry;
import de.ii.xsf.core.api.exceptions.ResourceExists;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import de.ii.xsf.core.api.organization.OrganizationDecider;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static de.ii.xsf.dropwizard.api.Dropwizard.FLAG_ALLOW_SERVICE_READDING;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.xsf.core.api.ServiceModule)",
        onArrival = "onModuleArrival",
        onDeparture = "onModuleDeparture")

public class ServiceRegistryDefault implements ServiceRegistry {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(ServiceRegistryDefault.class);
    private final static String DEFAULT_ORGANIZATION = "default_root_org";
    
    @Requires
    private Dropwizard cfg; 

    
    private final BundleContext context;

    private final Map<String, ServiceModule> serviceModules;

    //private final Map<String, Map<String, Service>> servicesOrg;
    
    public ServiceRegistryDefault(@Context BundleContext context) {
        //this.servicesOrg = new ConcurrentHashMap<>();
        this.serviceModules = new ConcurrentHashMap<>();
        //this.addOrganization(DEFAULT_ORGANIZATION);
        this.context = context;
    }

    public synchronized void onModuleArrival(ServiceReference<ServiceModule> ref) {
        ServiceModule mod = context.getService(ref);
        if (mod != null) {
            serviceModules.put(mod.getName().toLowerCase(), mod);
            /*try {

                Map<String, List<Service>> modServices = mod.getServices();
                for (String orgid : modServices.keySet()) {

                    
                    Map<String, Service> services = getServicesForOrgId(orgid);

                    //for (Service serv : modServices.get(orgid)) {

                        //services.put(serv.getId(), serv);
                        // TODO: autostart flag
                        //serv.start();

                    //}
                }
            } catch (IOException ex) {
                LOGGER.getLogger().error("Error loading services for module: {}", mod.getName(), ex);
            }*/
        }
    }

    public synchronized void onModuleDeparture(ServiceReference<ServiceModule> ref) {
        
        // TODO: typ ueber properties transportieren
        ServiceModule mod = context.getService(ref);
        if (mod != null) {
            serviceModules.remove(mod.getName().toLowerCase());
            /*try {
                for (List<Service> srves : mod.getServices().values()) {
                    for (Service s : srves) {
                        s.stop();
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(ServiceRegistryDefault.class.getName()).log(Level.SEVERE, null, ex);
            }*/
        }
    }

    /*private void addOrganization(String id) {
     this.servicesOrg.put(id, new ConcurrentHashMap<String, Service>());
     }
    
     private String normalizeOrgId(String id) {
     return id == null ? DEFAULT_ORGANIZATION : id;
     }*/
    private Map<String, Service> getServicesForOrgId(AuthenticatedUser authUser) {

        Map<String, Service> srvs = new HashMap<>();
        for (ServiceModule mod : serviceModules.values()) {
            for (Service s : mod.getServiceList(authUser)) {
                srvs.put(s.getId(), s);
            }
        }
        return srvs;
        /*
         String orgid = normalizeOrgId(id);
         if (!servicesOrg.containsKey(orgid)) {
         addOrganization(orgid);
         }
         return servicesOrg.get(orgid);
         */
    }

    @Override
    public List<Map<String, String>> getServiceTypes() {
        List<Map<String, String>> types = new ArrayList();
        for (ServiceModule mod : serviceModules.values()) {
            Map<String, String> type = new HashMap();
            type.put("id", mod.getName());
            types.add(type);
        }
        return types;
    }

    @Override
    public Collection<Service> getServices(AuthenticatedUser authUser) {

        return getServicesForOrgId(authUser).values();
    }

    @Override
    public void addService(AuthenticatedUser authUser, String type, String id, Map<String, String> params) {

        if (OrganizationDecider.isMultiTenancyRootOrg(authUser.getOrgId())) {
            throw new XtraserverFrameworkException("Adding Services to multi tenancy root organization is not allowed");
        }

        Map<String, Service> services = getServicesForOrgId(authUser);

        if (services.containsKey(id)) {
            if (!cfg.getFlags().get(FLAG_ALLOW_SERVICE_READDING)) {
                throw new ResourceExists("A Service with id '" + id + "' already exists.");
            }
            services.get(id).stop();
        }

        ServiceModule module = serviceModules.get(type.toLowerCase());

        Service srvc = null;

        try {
            srvc = module.addService(authUser, id, params, null);
        } catch (IOException ex) {
            //LOGGER.error(FrameworkMessages.IO_ERROR_WHILE_ADDING_SERVICE, ex);
            LOGGER.getLogger().error("Error adding service with id {}", id, ex);
            throw new WebApplicationException();
        }

        //services.put(id, srvc);
        //srvc.start();

        //for (ServiceCreationListener scl : serviceCreationListeners) {
        //scl.serviceCreated(srvc);
        //}
    }

    @Override
    public void updateService(AuthenticatedUser authUser, String id, Service update) {
        ServiceModule module = serviceModules.get(this.getService(authUser, id).getType().toLowerCase());

        module.updateService(authUser, id, update);
        //this.getServicesForOrgId(orgid).put(id, updated);
    }

    @Override
    public void deleteService(AuthenticatedUser authUser, Service service) {

        //getServicesForOrgId(orgid).get(service.getId()).stop();
        
        // remove from module
        ServiceModule module = serviceModules.get(service.getType().toLowerCase());
        module.deleteService(authUser, service);

        // remove from registry
       // Map<String, Service> services = getServicesForOrgId(orgid);
       
        //services.remove(service.getId());
    }

    @Override
    public Service getService(AuthenticatedUser authUser, String id) {
        Map<String, Service> services = getServicesForOrgId(authUser);
        if (services.containsKey(id)) {
            return services.get(id);
        } else {
            throw new ResourceNotFound("A service with id '" + id + "' is not available.");
        }
    }

}
